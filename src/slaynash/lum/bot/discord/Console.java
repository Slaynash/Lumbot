package slaynash.lum.bot.discord;

import java.util.Scanner;

public class Console
  extends Thread
  implements Runnable
{
  public Console()
  {
    setName("console");
    start();
  }
  
  @Override
public void run()
  {
    Scanner sc = new Scanner(System.in);
    for (;;)
    {
      String cmd = sc.nextLine();
      System.out.println(cmd);
      if (cmd.equals("stop"))
      {
        JDAManager.getJDA().shutdown();
        break;
      }
      if (cmd.startsWith("playing")) {
        //JDAManager.getJDA().getPresence().setGame(Game.of(Game.GameType.DEFAULT, cmd.split(" ", 2)[1]));
      }
    }
    sc.close();
  }
}
