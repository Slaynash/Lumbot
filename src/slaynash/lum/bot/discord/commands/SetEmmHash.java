package slaynash.lum.bot.discord.commands;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import slaynash.lum.bot.discord.Command;
import slaynash.lum.bot.discord.CommandManager;
import slaynash.lum.bot.discord.JDAManager;

public class SetEmmHash extends Command {
    
    @Override
    protected void onServer(String paramString, MessageReceivedEvent event) {
        if (!checkPerms(event))
            return;

        String[] parts = paramString.split(" ", 2);
        
        try {
            String parm = parts[1].toLowerCase().trim();

            if(!parm.matches("([0-9a-f]{31,})")){
                event.getChannel().sendMessageEmbeds(JDAManager.wrapMessageInEmbed("Please make sure " + parts[1] + " is a valid hash.", Color.ORANGE)).queue();
                return;
            }

            CommandManager.emmVRCHash = parm;
            
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("emmvrchash.txt"))) {
                writer.write(parm);
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            event.getChannel().sendMessageEmbeds(JDAManager.wrapMessageInEmbed("emmVRC hash is now set to " + CommandManager.emmVRCHash, Color.GREEN)).queue();
            
        }
        catch (Exception e) {
            event.getChannel().sendMessageEmbeds(JDAManager.wrapMessageInEmbed("**Usage**:\nl!setemmhash <hash>", Color.RED)).queue();
        }
    }
    
    @Override
    protected boolean matchPattern(String paramString) {
        return paramString.split(" ", 2)[0].equals("l!setemmhash");
    }
    
    
    private boolean checkPerms(MessageReceivedEvent event) {
        Long category = event.getMessage().getCategory() == null ? 0L : event.getMessage().getCategory().getIdLong();
        if (category == 600914209303298058L)
            return true;
        else
            return false;
    }
    
    @Override
    public boolean includeInHelp(MessageReceivedEvent event) {
        return checkPerms(event);
    }
    
    @Override
    public String getHelpDescription() {
        return "Sets the latest emmVRC hash";
    }
    
    @Override
    public String getHelpName() {
        return "l!setemmhash";
    }
}
