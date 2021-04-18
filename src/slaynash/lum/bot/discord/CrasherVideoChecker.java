package slaynash.lum.bot.discord;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;

import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class CrasherVideoChecker {

    public static void check(MessageReceivedEvent event) {

        List<Attachment> attachments = event.getMessage().getAttachments();

        for (int i = 0; i < attachments.size(); ++i) {
            Attachment attachment = attachments.get(i);
            if (attachment.getFileExtension().toLowerCase().equals("mp4")) {
                try (InputStream is = attachment.retrieveInputStream().get()) {
                    if (checkForCrasher(is, event))
                        return;
                }
                catch (InterruptedException | ExecutionException | IOException e) {
                    e.printStackTrace();
                }
            }
        }

        String content = event.getMessage().getContentRaw();
        String[] words = content.split("[\\[\\]\\(\\)\\s<>\\|]");
        for (String word : words) {
            word = word.trim();
            if (word.matches("https?://.*")) {
                String url = (word.contains("http:\\/\\/") ? "http://" : "https://") + word.split("https?:\\/\\/", 3)[1];
                System.out.println("weblink: " + url);
                try (InputStream is = new URL(url).openStream()) {
                    byte[] bytes = is.readNBytes(8);
                    if (bytes[4] == 0x66 && bytes[5] == 0x74 && bytes[6] == 0x79 && bytes[7] == 0x70 && checkForCrasher(is, event))
                        return;
                    
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            String[] tags = line.split("<");
                            for (String tag : tags) {
                                //if (tag.contains("og"))
                                //    System.out.println("[" + (tag.contains("og:video") && !tag.contains("og:video:")) + "] " + tag);
                                if (tag.contains("og:video") && !tag.contains("og:video:")) {
                                    String videourl;
                                    if (tag.contains("http://"))
                                        videourl = "http://";
                                    else if (tag.contains("https://"))
                                        videourl = "https://";
                                    else
                                        continue;
                                    
                                    videourl += tag.split("https?:\\/\\/", 3)[1].split("\"")[0];
                                    System.out.println("videourl: " + videourl);
                                    try (InputStream vis = new URL(videourl).openStream()) {
                                        if (checkForCrasher(vis, event))
                                            return;
                                    }
                                    catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private static boolean checkForCrasher(InputStream is, MessageReceivedEvent event) throws IOException {
        int totalRead = 0;
        boolean donereading = false;
        while (!donereading && totalRead < 10_000) {
            int bufferIndex = 0;
            byte[] buffer = new byte[10000];
            while (bufferIndex < buffer.length) {
                int read = is.read(buffer, bufferIndex, buffer.length - bufferIndex);
                if (read == -1) {
                    donereading = true;
                    break;
                }

                bufferIndex += read;
                totalRead += read;
            }
            
            if (indexOf(buffer, bufferIndex, new byte[] {0x41, 0x56, 0x43}) > 128) {
                event.getMessage().delete().queue();
                event.getChannel().sendMessage(JDAManager.wrapMessageInEmbed("<@!" + event.getMessage().getMember().getId() + "> tried to post a crasher video", Color.RED)).queue();
                if (event.getGuild().getIdLong() == 439093693769711616L /* VRCMG */ ||
                    event.getGuild().getIdLong() == 600298024425619456L /* emmVRC */ ||
                    event.getGuild().getIdLong() == 663449315876012052L /* MelonLoader */)
                    event.getGuild().ban(event.getMember(), 0, "Posting a crasher video").queue();
                return true;
            }
        }

        return false;
    }

    private static int indexOf(byte[] outerArray, int outerArraySize, byte[] smallerArray) {
        for (int i = 0; i < outerArraySize - smallerArray.length + 1; ++i) {
            boolean found = true;
            for(int j = 0; j < smallerArray.length; ++j) {
                if (outerArray[i + j] != smallerArray[j]) {
                    found = false;
                    break;
                }
            }
            if (found)
                return i;
        }
        return -1;  
    }  

}
