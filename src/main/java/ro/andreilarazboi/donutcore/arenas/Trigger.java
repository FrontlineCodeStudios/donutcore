package ro.andreilarazboi.donutcore.arenas;

public class Trigger {

    private final String name;
    private String region;
    private boolean enabled = true;

    // ── Command action ────────────────────────────────────────────────────────
    private boolean commandEnabled = false;
    private String  command        = "";

    // ── Teleport action (fixed location, captured from an admin standing there) ─
    private boolean teleportEnabled = false;
    private String  teleportWorld   = "";
    private double  teleportX       = 0.0;
    private double  teleportY       = 0.0;
    private double  teleportZ       = 0.0;
    private float   teleportYaw     = 0.0f;
    private float   teleportPitch   = 0.0f;

    // ── Message on enter ──────────────────────────────────────────────────────
    private String messageType = "none"; // none | chat | actionbar | title
    private String message     = "";
    private String title       = "";
    private String subtitle    = "";

    public Trigger(String name, String region) {
        this.name   = name;
        this.region = region;
    }

    public String  getName()              { return name; }
    public String  getRegion()            { return region; }
    public void    setRegion(String r)    { this.region = r; }
    public boolean isEnabled()            { return enabled; }
    public void    setEnabled(boolean e)  { this.enabled = e; }

    public boolean isCommandEnabled()             { return commandEnabled; }
    public void    setCommandEnabled(boolean e)   { this.commandEnabled = e; }
    public String  getCommand()                   { return command; }
    public void    setCommand(String c)           { this.command = c; }

    public boolean isTeleportEnabled()            { return teleportEnabled; }
    public void    setTeleportEnabled(boolean e)  { this.teleportEnabled = e; }
    public String  getTeleportWorld()             { return teleportWorld; }
    public void    setTeleportWorld(String w)     { this.teleportWorld = w; }
    public double  getTeleportX()                 { return teleportX; }
    public void    setTeleportX(double x)         { this.teleportX = x; }
    public double  getTeleportY()                 { return teleportY; }
    public void    setTeleportY(double y)         { this.teleportY = y; }
    public double  getTeleportZ()                 { return teleportZ; }
    public void    setTeleportZ(double z)         { this.teleportZ = z; }
    public float   getTeleportYaw()               { return teleportYaw; }
    public void    setTeleportYaw(float y)        { this.teleportYaw = y; }
    public float   getTeleportPitch()             { return teleportPitch; }
    public void    setTeleportPitch(float p)      { this.teleportPitch = p; }
    public boolean hasTeleportLocation()          { return teleportWorld != null && !teleportWorld.isBlank(); }

    public String getMessageType()           { return messageType; }
    public void   setMessageType(String t)   { this.messageType = t; }
    public String getMessage()               { return message; }
    public void   setMessage(String m)       { this.message = m; }
    public String getTitle()                 { return title; }
    public void   setTitle(String t)         { this.title = t; }
    public String getSubtitle()              { return subtitle; }
    public void   setSubtitle(String s)      { this.subtitle = s; }
}
