package org.jenkinsci.plugins.compareplugins;

/**
 * Created by alojoshi on 3/10/17.
 */
public final class PluginInfo implements Comparable<PluginInfo>{
    private String shortName;
    private boolean enabled;
    private String url;
    private String version;

    public PluginInfo(String shortName, boolean enabled, String url, String version) {
        this.shortName = shortName;
        this.enabled = enabled;
        this.url = url;
        this.version = version;
    }

    public String getShortName() {
        return shortName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getUrl() {
        return url;
    }

    public String getVersion() {
        return version;
    }

    public int compareTo(final PluginInfo other) {
        if (this == other) {
            return 0;
        }

        return shortName.compareTo(other.getShortName());
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof PluginInfo)) {
            return false;
        }

        return shortName.equals(((PluginInfo) obj).getShortName());
    }

    @Override
    public int hashCode() {
        return shortName.hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder().append("RemotePlugin: ").append(shortName).append(", ").append(url).append(", ")
                .append(version).toString();
    }
}
