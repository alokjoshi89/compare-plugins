package org.jenkinsci.plugins.compareplugins;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.google.common.base.Strings;
import hudson.Extension;
import hudson.Plugin;
import hudson.PluginWrapper;
import hudson.model.*;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Alok Joshi
 */
@Extension
public class ComparePluginsAction implements RootAction, Describable<ComparePluginsAction> {

    private static final Logger LOG = Logger.getLogger(ComparePluginsAction.class.getName());

    private String remoteUrl;
    private String credentialId;

    private final SortedSet<PluginInfo> remotePlugins = new TreeSet<>();
    private final SortedSet<PluginInfo> hostPlugins = new TreeSet<>();
    private final SortedSet<PluginInfo> diffPlugins = new TreeSet<>();

    public String getUrlName() {
        return "/compare-plugins";
    }

    public String getDisplayName() {
        return "Compare Plugins";
    }

    public String getIconFileName() {
        return "/images/32x32/setting.png";
    }

    public void getRemotePlugins(String remoteUrl, String username, String password) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(URLUtils.fetchUrl(remoteUrl + "/pluginManager/api/xml?depth=1", username, password));
            NodeList nl = doc.getElementsByTagName("plugin");

            for (int i = 0; i < nl.getLength(); i++) {
                boolean isEnabled = false;
                Element plugin = (Element) nl.item(i);
                String shortName = text(plugin, "shortName");
                String enabled = text(plugin, "enabled");
                if (enabled.equalsIgnoreCase("true") || enabled.equalsIgnoreCase("false")) {
                    isEnabled = Boolean.valueOf(enabled);
                }
                String url = text(plugin, "url");
                String version = text(plugin, "version");
                PluginInfo remotePlugin = new PluginInfo(shortName, isEnabled, url, version);
                remotePlugins.add(remotePlugin);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, (new StringBuilder()).append("Failed to fetch plugins from remote ").append(remoteUrl).toString(), e);
        }
    }

    public void getHostPlugins() {
        try {
            Jenkins current = Jenkins.getActiveInstance();
            List<Plugin> plugins = current.getPlugins(Plugin.class);
            for (Plugin plugin : plugins) {
                PluginWrapper pluginWrapper = plugin.getWrapper();
                hostPlugins.add(new PluginInfo(pluginWrapper.getShortName(), pluginWrapper.isEnabled(), pluginWrapper.getUrl(), pluginWrapper.getVersion()));
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, (new StringBuilder()).append("Failed to fetch plugins from host ").toString(), e);
        }
    }

    public SortedSet<PluginInfo> getDiffPlugins() {
        return diffPlugins;
    }

    public void getPluginsDifference() {
        diffPlugins.addAll(remotePlugins);
        diffPlugins.removeAll(hostPlugins);
    }

    public void doClear(final StaplerRequest request, final StaplerResponse response) throws ServletException,
            IOException {
        remoteUrl = null;
        credentialId = null;
        remotePlugins.clear();
        hostPlugins.clear();
        diffPlugins.clear();
        response.sendRedirect(Jenkins.getActiveInstance().getRootUrl());
    }

    public void doQuery(final StaplerRequest request, final StaplerResponse response) throws ServletException,
            IOException {
        remotePlugins.clear();
        hostPlugins.clear();
        diffPlugins.clear();
        remoteUrl = request.getParameter("remoteUrl");
        String username = "";
        String password = "";
        credentialId = request.getParameter("_.credentialId");

        if (!Strings.isNullOrEmpty(credentialId)) {
            StandardUsernamePasswordCredentials cred = CredentialsMatchers.firstOrNull(
                    allCredentials(),
                    CredentialsMatchers.withId(credentialId)
            );
            if (cred != null) {
                username = cred.getUsername();
                password = cred.getPassword().getPlainText();
            }
        }

        if (StringUtils.isNotEmpty(remoteUrl)) {
            getRemotePlugins(remoteUrl, username, password);
            getHostPlugins();
            getPluginsDifference();
        }

        response.forwardToPreviousPage(request);
    }

    private static String text(Element e, String name) {
        NodeList nl = e.getElementsByTagName(name);
        if (nl.getLength() == 1) {
            Element e2 = (Element) nl.item(0);
            return e2.getTextContent();
        } else {
            return null;
        }
    }

    public String getRootUrl() {
        return Jenkins.getActiveInstance().getRootUrl();
    }

    public String getRemoteUrl() { return remoteUrl; }

    public void setRemoteUrl(String remoteUrl) { this.remoteUrl = remoteUrl; }

    public String getCredentialId() { return credentialId; }

    public FormValidation doTestConnection(@QueryParameter("remoteUrl") final String remoteUrl) {
        return FormValidation.ok();
    }

    public boolean isPluginsDiffer() {
        return diffPlugins.size() > 0;
    }

    private static List<StandardUsernamePasswordCredentials> allCredentials() {
        return CredentialsProvider.lookupCredentials(
                StandardUsernamePasswordCredentials.class,
                (Item) null,
                ACL.SYSTEM,
                Collections.<DomainRequirement>emptyList()
        );
    }

    @Override
    public Descriptor<ComparePluginsAction> getDescriptor() {
        Jenkins jenkins = Jenkins.getActiveInstance();
        if (jenkins == null) {
            throw new IllegalStateException("Jenkins has not been started, or was already shut down");
        }
        return jenkins.getDescriptorOrDie(getClass());
    }


    @Extension
    public static final class ComparePluginsActionDescriptor extends Descriptor<ComparePluginsAction> {

        @Override
        public String getDisplayName() { return ""; }

        public ListBoxModel doFillCredentialIdItems() {
            return new StandardUsernameListBoxModel()
                    .withEmptySelection()
                    .withAll(allCredentials());
        }
    }

}

