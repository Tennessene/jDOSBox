package jdos.shell;

public class SHELL_Cmd {
    public SHELL_Cmd(String name, int flags, Dos_shell.handler handler, String help) {
        this.name = name;
        this.flags = flags;
        this.handler = handler;
        this.help = help;
    }
    public String name;								/* Command name*/
    public /*Bit32u*/int flags;									/* Flags about the command */
    public Dos_shell.handler handler;		/* Handler for this command */
    public String help;								/* String with command help */
}
