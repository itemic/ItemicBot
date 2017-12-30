import com.google.common.primitives.Chars;
import com.vdurmont.emoji.EmojiManager;
import com.vdurmont.emoji.EmojiParser;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

public class ItemicBot extends TelegramLongPollingBot{

    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {



            String messageText = update.getMessage().getText();

            if (messageText.startsWith(Commands.BMOJIFY)) {
                messageText = messageText.substring(Commands.BMOJIFY.length());
                long chat_id = update.getMessage().getChatId();
                messageText = messageText.replaceAll("([b|B])+", "&#127345;");
                messageText = EmojiParser.parseToUnicode(messageText);
                System.out.println(messageText);
                SendMessage message = new SendMessage().setChatId(chat_id).setText(messageText);

                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (messageText.startsWith(Commands.WAKA)) {
                String locationToken = messageText.split(" ")[1];
                long chat_id = update.getMessage().getChatId();
                String msgResponse = "";
                try {
                    String stationUrl = "https://getwaka.com/a/nz-akl/station/" + locationToken;
                    String timesUrl = stationUrl + "/times/";

                    CloseableHttpClient client = HttpClients.createDefault();
                    HttpGet httpGet = new HttpGet(stationUrl);
                    CloseableHttpResponse response = client.execute(httpGet);

                    String rawJson = IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
                    JSONObject json = new JSONObject(rawJson);
                    response.close();
                    String stationName = json.optString("stop_name", "STATION_DOES_NOT_EXIST");

                    if (stationName.equals("STATION_DOES_NOT_EXIST")) {
                        msgResponse = "**Error! That's not a station!**";
                    } else {
                        msgResponse = stationName;
                        StringBuilder realTimeResponse = new StringBuilder("*" + stationName + " (" + locationToken + ")*" + "\n");
                        httpGet = new HttpGet(timesUrl);
                        response = client.execute(httpGet);
                        JSONObject timesJson = new JSONObject(IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset()));
                        int currentTime = timesJson.getInt("currentTime");
                        JSONArray tripsArray = timesJson.getJSONArray("trips");
                        System.out.println(tripsArray);
                        for (int i = 0; i < Math.min(5, tripsArray.length()); i++) {
                            JSONObject trip = tripsArray.getJSONObject(i);
                            String headSign = trip.optString("trip_headsign", "ERR");
                            String route = trip.optString("route_short_name", "ERR");
                            int departureTime = trip.optInt("departure_time_seconds");
                            if (currentTime > departureTime || (departureTime-currentTime > 7200 ) ) {
//                                realTimeResponse.append("There are no services in the next 2 hours.\n");
                            } else {
                                String timeUntilLeave = WakaHandler.logicalTime(currentTime, departureTime);

                                String entry = "[" + route + "] to " + headSign + " due to depart in " + timeUntilLeave + ".";
                                realTimeResponse.append(entry + "\n");
                            }
                        }
                        response.close();
                        realTimeResponse.append("\n_Waka for Telegram is still under BETA testing._");
                        msgResponse = realTimeResponse.toString();
                    }


                    SendMessage message = new SendMessage().setChatId(chat_id).setText(msgResponse).setParseMode("Markdown");
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

    }

    public String getBotUsername() {
        return "ItemicBot";
    }

    public String getBotToken() {
        return Token.ITEMIC_BOT_API_KEY; // put your own key here :)
    }
}
