package com.github.havlli.EventPilot.entity.guild;

import java.util.Objects;

public class Guild {

    private String snowflakeId;
    private String guildName;

    public Guild() {
    }

    public Guild(String snowflakeId, String guildName) {
        this.snowflakeId = snowflakeId;
        this.guildName = guildName;
    }

    public String getSnowflakeId() {
        return snowflakeId;
    }

    public void setSnowflakeId(String snowflakeId) {
        this.snowflakeId = snowflakeId;
    }

    public String getGuildName() {
        return guildName;
    }

    public void setGuildName(String guildName) {
        this.guildName = guildName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Guild guild = (Guild) o;
        return Objects.equals(snowflakeId, guild.snowflakeId) && Objects.equals(guildName, guild.guildName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(snowflakeId, guildName);
    }

    @Override
    public String toString() {
        return "Guild{" +
                "snowflakeId='" + snowflakeId + '\'' +
                ", guildName='" + guildName + '\'' +
                '}';
    }
}
