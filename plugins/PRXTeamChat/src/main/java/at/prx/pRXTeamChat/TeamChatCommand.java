package at.prx.pRXTeamChat;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
public class TeamChatCommand implements SimpleCommand {

    private final PRXTeamChat plugin;

    public TeamChatCommand(PRXTeamChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission(PRXTeamChat.PERMISSION_USE)) {
            plugin.sendSystemMessage(source, "&cDu hast keine Rechte dafuer.");
            return;
        }

        if (args.length == 0) {
            plugin.sendSystemMessage(source, "&cNutze: /tc <nachricht>");
            return;
        }

        String message = String.join(" ", args);
        plugin.broadcastTeamChat(source, message);
    }
}
