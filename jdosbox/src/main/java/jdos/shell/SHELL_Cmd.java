package jdos.shell;

public class SHELL_Cmd {
    public SHELL_Cmd(String name, int flags, Dos_shell.handler handler, String help) {
        this.name = name;
        this.flags = flags;
        this.handler = handler;
        this.help = help;
    }
    public final String name;								/* Command name*/
    public final /*Bit32u*/int flags;									/* Flags about the command */
    public final Dos_shell.handler handler;		/* Handler for this command */
    public final String help;								/* String with command help */
}
