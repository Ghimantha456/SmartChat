package controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import entity.Chat;
import entity.User;
import entity.User_Status;
import java.io.File;
import java.io.IOException;
//import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.HibernateUtil;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

@WebServlet(name = "LoadHomeData", urlPatterns = {"/LoadHomeData"})
public class LoadHomeData extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        Gson gson = new Gson();

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("success", false);
        responseJson.addProperty("message", "Unable to process your request");

        try {

            Session session = HibernateUtil.getSessionFactory().openSession();
            //get user id from req parameter
            String userId = req.getParameter("id");

            //get user object
            User user = (User) session.get(User.class, Integer.parseInt(userId));

            //get user status = 1 (online)
            User_Status user_Status = (User_Status) session.get(User_Status.class, 1);

            //update user status
            user.setUser_status(user_Status);
            session.update(user);

            //get other users
            Criteria criteria1 = session.createCriteria(User.class);
            criteria1.add(Restrictions.ne("id", user.getId()));

            List<User> otherUserList = criteria1.list();
            JsonArray jsonChatArray = new JsonArray();

            //remove passwords
            for (User otherUser : otherUserList) {

                //get last conversation
                Criteria criteria2 = session.createCriteria(Chat.class);
                criteria2.add(
                        Restrictions.or(
                                Restrictions.and(
                                        Restrictions.eq("from_user", user),
                                        Restrictions.eq("to_user", otherUser)
                                ),
                                Restrictions.and(
                                        Restrictions.eq("from_user", otherUser),
                                        Restrictions.eq("to_user", user)
                                )
                        )
                );
                criteria2.addOrder(Order.desc("id"));
                criteria2.setMaxResults(1);

                //create chat item json to send frontend data
                JsonObject chatItem = new JsonObject();
                chatItem.addProperty("other_user_id", otherUser.getId());
                chatItem.addProperty("other_user_mobile", otherUser.getMobile());
                chatItem.addProperty("other_user_name", otherUser.getFirst_name() + " " + otherUser.getLast_name());
                chatItem.addProperty("other_user_status", otherUser.getUser_status().getId());  //1 = online, 2 = offline

                //check avatar image
                String serverPath = req.getServletContext().getRealPath("");
                String otherUserAvatarImagePath = serverPath + File.separator + "AvatarImages" + File.separator + otherUser.getMobile() + ".png";
                File otherUserAvatarImageFile = new File(otherUserAvatarImagePath);

                if (otherUserAvatarImageFile.exists()) {
                    //avatar image found
                    chatItem.addProperty("avatar_image_found", true);

                } else {
                    //avatar Image not found
                    chatItem.addProperty("avatar_image_found", false);
                    chatItem.addProperty("other_user_avatar_letters", otherUser.getFirst_name().charAt(0) + "" + otherUser.getLast_name().charAt(0));
                }

                //get chat list
                List<Chat> dbChatList = criteria2.list();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy, MMM dd hh:ss a");

                if (dbChatList.isEmpty()) {
                    //no chat
                    chatItem.addProperty("message", "Let's start new convesation");
                    chatItem.addProperty("dateTime", dateFormat.format(user.getRegistered_date_time()));
                    chatItem.addProperty("chat_Status_id", 1); //1 = seen, 2 = unseen

                } else {
                    //found last chat
                    chatItem.addProperty("message", dbChatList.get(0).getMessage());
                    chatItem.addProperty("dateTime", dateFormat.format(dbChatList.get(0).getDate_time()));
                    chatItem.addProperty("chat_status_id", dbChatList.get(0).getChat_status().getId()); //1 = seen, 2 = unseen

                }
                //get last conversation
                jsonChatArray.add(chatItem);
            }

            //send users
            responseJson.addProperty("success", true);
            responseJson.addProperty("message", "Success");

            responseJson.add("jsonChatArray", gson.toJsonTree(jsonChatArray));

            session.beginTransaction().commit();
            session.close();

        } catch (NumberFormatException | HibernateException e) {
        }

        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(responseJson));

    }
}
