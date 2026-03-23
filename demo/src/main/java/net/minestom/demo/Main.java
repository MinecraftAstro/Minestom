package net.minestom.demo;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.demo.commands.*;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;

public class Main {

    static void main(String[] args) {
        MinecraftServer minecraftServer = MinecraftServer.init(new Auth.Online());

        CommandManager commandManager = MinecraftServer.getCommandManager();
        commandManager.register(new AroundCommand());
        commandManager.register(new ComeCommand());
        commandManager.register(new TestCommand());
        commandManager.register(new EntitySelectorCommand());
        commandManager.register(new HealthCommand());
        commandManager.register(new LegacyCommand());
        commandManager.register(new DimensionCommand());
        commandManager.register(new ShutdownCommand());
        commandManager.register(new TeleportCommand());
        commandManager.register(new PlayersCommand());
        commandManager.register(new FindCommand());
        commandManager.register(new TitleCommand());
        commandManager.register(new BookCommand());
        commandManager.register(new ShootCommand());
        commandManager.register(new HorseCommand());
        commandManager.register(new EchoCommand());
        commandManager.register(new SummonCommand());
        commandManager.register(new SummonRandomCommand());
        commandManager.register(new RemoveCommand());
        commandManager.register(new GiveCommand());
        commandManager.register(new SetBlockCommand());
        commandManager.register(new AutoViewCommand());
        commandManager.register(new SaveCommand());
        commandManager.register(new GamemodeCommand());
        commandManager.register(new ExecuteCommand());
        commandManager.register(new RedirectTestCommand());
        commandManager.register(new DebugGridCommand());
        commandManager.register(new DisplayCommand());
        commandManager.register(new NotificationCommand());
        commandManager.register(new TestCommand2());
        commandManager.register(new ConfigCommand());
        commandManager.register(new SidebarCommand());
        commandManager.register(new SetEntityType());
        commandManager.register(new RelightCommand());
        commandManager.register(new KillCommand());
        commandManager.register(new WeatherCommand());
        commandManager.register(new PotionCommand());
        commandManager.register(new CookieCommand());
        commandManager.register(new WorldBorderCommand());
        commandManager.register(new TestInstabreakCommand());
        commandManager.register(new AttributeCommand());
        commandManager.register(new PrimedTNTCommand());
        commandManager.register(new SleepCommand());
        commandManager.register(new MinecartCommand());
        commandManager.register(new BelowNameCommand());
        commandManager.register(new InventoryCommand());

        commandManager.setUnknownCommandCallback((sender, command) -> sender.sendMessage(Component.text("Unknown command", NamedTextColor.RED)));

        new PlayerInit().init();

        minecraftServer.start("0.0.0.0", 25565);
    }
}