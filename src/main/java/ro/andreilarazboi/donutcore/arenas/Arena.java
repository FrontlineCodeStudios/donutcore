package ro.andreilarazboi.donutcore.arenas;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class Arena {

    private final String name;
    private String region;
    private String worldName;
    private final Map<String, Trigger> triggers = new LinkedHashMap<>();

    // ── AFK Timer ─────────────────────────────────────────────────────────────
    private boolean afkEnabled         = false;
    private int     afkIntervalSeconds = 60;
    private String  afkCommand         = "";
    private String  afkDisplayType     = "actionbar"; // actionbar | title | chat | all
    private String  afkActionbarText   = "&aAFK Zone &7| Reward in &e{time}";
    private String  afkTitleText       = "&aAFK Zone";
    private String  afkSubtitleText    = "&7Reward in &e{time}";
    private String  afkChatText        = "&7[Zone] Reward in &e{time}";

    // ── Enter action ──────────────────────────────────────────────────────────
    private boolean enterCommandEnabled = false;
    private String  enterCommand        = "";
    private String  enterMessageType    = "chat"; // none | chat | actionbar | title
    private String  enterMessage        = "&aYou entered the &e{zone} &azone.";
    private String  enterTitle          = "&a{zone}";
    private String  enterSubtitle       = "&7Welcome!";

    // ── Exit action ───────────────────────────────────────────────────────────
    private boolean exitCommandEnabled  = false;
    private String  exitCommand         = "";
    private String  exitMessageType     = "chat"; // none | chat | actionbar | title
    private String  exitMessage         = "&7You left the &e{zone} &7zone.";
    private String  exitTitle           = "&7{zone}";
    private String  exitSubtitle        = "&7Goodbye!";

    public Arena(String name, String region, String worldName) {
        this.name      = name;
        this.region    = region;
        this.worldName = worldName;
    }

    // ── Basic ──────────────────────────────────────────────────────────────────
    public String getName()             { return name; }
    public String getRegion()           { return region; }
    public void   setRegion(String r)   { this.region = r; }
    public String getWorldName()        { return worldName; }
    public void   setWorldName(String w){ this.worldName = w; }

    // ── AFK Timer ──────────────────────────────────────────────────────────────
    public boolean isAfkEnabled()                   { return afkEnabled; }
    public void    setAfkEnabled(boolean e)         { this.afkEnabled = e; }
    public int     getAfkIntervalSeconds()          { return afkIntervalSeconds; }
    public void    setAfkIntervalSeconds(int s)     { this.afkIntervalSeconds = s; }
    public String  getAfkCommand()                  { return afkCommand; }
    public void    setAfkCommand(String c)          { this.afkCommand = c; }
    public String  getAfkDisplayType()              { return afkDisplayType; }
    public void    setAfkDisplayType(String t)      { this.afkDisplayType = t; }
    public String  getAfkActionbarText()            { return afkActionbarText; }
    public void    setAfkActionbarText(String t)    { this.afkActionbarText = t; }
    public String  getAfkTitleText()                { return afkTitleText; }
    public void    setAfkTitleText(String t)        { this.afkTitleText = t; }
    public String  getAfkSubtitleText()             { return afkSubtitleText; }
    public void    setAfkSubtitleText(String t)     { this.afkSubtitleText = t; }
    public String  getAfkChatText()                 { return afkChatText; }
    public void    setAfkChatText(String t)         { this.afkChatText = t; }

    // ── Enter action ───────────────────────────────────────────────────────────
    public boolean isEnterCommandEnabled()              { return enterCommandEnabled; }
    public void    setEnterCommandEnabled(boolean e)    { this.enterCommandEnabled = e; }
    public String  getEnterCommand()                    { return enterCommand; }
    public void    setEnterCommand(String c)            { this.enterCommand = c; }
    public String  getEnterMessageType()                { return enterMessageType; }
    public void    setEnterMessageType(String t)        { this.enterMessageType = t; }
    public String  getEnterMessage()                    { return enterMessage; }
    public void    setEnterMessage(String m)            { this.enterMessage = m; }
    public String  getEnterTitle()                      { return enterTitle; }
    public void    setEnterTitle(String t)              { this.enterTitle = t; }
    public String  getEnterSubtitle()                   { return enterSubtitle; }
    public void    setEnterSubtitle(String t)           { this.enterSubtitle = t; }

    // ── Exit action ────────────────────────────────────────────────────────────
    public boolean isExitCommandEnabled()               { return exitCommandEnabled; }
    public void    setExitCommandEnabled(boolean e)     { this.exitCommandEnabled = e; }
    public String  getExitCommand()                     { return exitCommand; }
    public void    setExitCommand(String c)             { this.exitCommand = c; }
    public String  getExitMessageType()                 { return exitMessageType; }
    public void    setExitMessageType(String t)         { this.exitMessageType = t; }
    public String  getExitMessage()                     { return exitMessage; }
    public void    setExitMessage(String m)             { this.exitMessage = m; }
    public String  getExitTitle()                       { return exitTitle; }
    public void    setExitTitle(String t)               { this.exitTitle = t; }
    public String  getExitSubtitle()                    { return exitSubtitle; }
    public void    setExitSubtitle(String t)            { this.exitSubtitle = t; }

    // ── Triggers (sub-region enter actions) ──────────────────────────────────────
    public void addTrigger(Trigger trigger)        { triggers.put(trigger.getName().toLowerCase(), trigger); }
    public void removeTrigger(String name)          { triggers.remove(name == null ? null : name.toLowerCase()); }
    public Trigger getTrigger(String name)          { return name == null ? null : triggers.get(name.toLowerCase()); }
    public Collection<Trigger> getTriggers()        { return triggers.values(); }
}
