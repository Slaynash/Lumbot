package slaynash.lum.bot.discord;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public abstract class LuaPackages
{
  private static class PrintFunction
    extends OneArgFunction
  {
    private MessageChannel channel;
    
    public PrintFunction(MessageChannel channel)
    {
      this.channel = channel;
    }
    
    @Override
	public LuaValue call(LuaValue arg)
    {
      this.channel.sendMessage(arg.toString()).queue();
      return LuaValue.NIL;
    }
  }
  
  private static class getChannelIdFunction extends ZeroArgFunction
  {
    private MessageChannel channel;
    
    public getChannelIdFunction(MessageChannel channel)
    {
      this.channel = channel;
    }
    
    @Override
	public LuaValue call()
    {
      return LuaValue.valueOf(this.channel.getId());
    }
  }
  
  private static class getChannelNameFunction
    extends ZeroArgFunction
  {
    private MessageChannel channel;
    
    public getChannelNameFunction(MessageChannel channel)
    {
      this.channel = channel;
    }
    
    @Override
	public LuaValue call()
    {
      return LuaValue.valueOf(this.channel.getName());
    }
  }
  
  private static class getAuthorIdFunction extends ZeroArgFunction
  {
    private User user;
    
    public getAuthorIdFunction(User user)
    {
      this.user = user;
    }
    
    @Override
	public LuaValue call()
    {
      return LuaValue.valueOf(this.user.getId());
    }
  }
  
  private static class getArgumentsFunction
    extends ZeroArgFunction
  {
    private LuaValue args;
    
    public getArgumentsFunction(String[] args)
    {
      LuaValue[] argList = new LuaValue[args.length];
      for (int i = 0; i < args.length; i++) {
        argList[i] = LuaValue.valueOf(args[i]);
      }
      this.args = LuaValue.listOf(argList);
    }
    
    @Override
	public LuaValue call()
    {
      return this.args;
    }
  }
  
  private static class isChannelNSFWFunction
    extends ZeroArgFunction
  {
    private TextChannel channel;
    
    public isChannelNSFWFunction(TextChannel channel)
    {
      this.channel = channel;
    }
    
    @Override
	public LuaValue call()
    {
      return this.channel.isNSFW() ? LuaValue.TRUE : LuaValue.FALSE;
    }
  }
  
  public static Globals createRunGlobals(MessageReceivedEvent event)
  {
    Globals m_globals = JsePlatform.standardGlobals();
    LuaValue discord = new LuaTable();
    
    m_globals.set("discord", discord);
    discord.set("print", new PrintFunction(event.getChannel()));
    discord.set("getChannelId", new getChannelIdFunction(event.getChannel()));
    discord.set("getChannelName", new getChannelNameFunction(event.getChannel()));
    discord.set("getAuthorId", new getAuthorIdFunction(event.getAuthor()));
    discord.set("isChannelNSFW", new isChannelNSFWFunction(event.getTextChannel()));
    if (event.getMessage().getContentRaw().split(" ", 2).length > 1) {
      discord.set("getArguments", new getArgumentsFunction(event.getMessage().getContentRaw().split(" ", 2)[1].split(" ")));
    } else {
      discord.set("getArguments", new getArgumentsFunction(new String[0]));
    }
    return m_globals;
  }
  
  public static Globals createCommandGlobals(MessageReceivedEvent event)
  {
    Globals m_globals = JsePlatform.standardGlobals();
    LuaValue discord = new LuaTable();
    m_globals.set("discord", discord);
    discord.set("print", new PrintFunction(event.getChannel()));
    discord.set("getChannelId", new getChannelIdFunction(event.getChannel()));
    discord.set("getChannelName", new getChannelNameFunction(event.getChannel()));
    discord.set("getAuthorId", new getAuthorIdFunction(event.getAuthor()));
    discord.set("isChannelNSFW", new isChannelNSFWFunction(event.getTextChannel()));
    if (event.getMessage().getContentRaw().toLowerCase().substring(2).split(" ", 2).length > 1) {
      discord.set("getArguments", new getArgumentsFunction(event.getMessage().getContentRaw().substring(2).split(" ", 2)[1].split(" ")));
    } else {
      discord.set("getArguments", new getArgumentsFunction(new String[0]));
    }
    return m_globals;
  }
}
