package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import entity.User;
import entity.User_Status;
//import java.io.File;
import java.io.IOException;
//import java.io.PrintWriter;
//import java.nio.file.Files;
//import java.nio.file.StandardCopyOption;
//import java.text.SimpleDateFormat;
//import java.util.Date;
import javax.servlet.ServletException;
//import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
//import javax.servlet.http.Part;
import model.HibernateUtil;
import model.Validations;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

@WebServlet(name = "SignIn", urlPatterns = {"/SignIn"})
public class SignIn extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        Gson gson = new Gson();
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("success", false);

        JsonObject requestJson = gson.fromJson(req.getReader(), JsonObject.class);
        String mobile = requestJson.get("mobile").getAsString();
        String password = requestJson.get("password").getAsString();

        if (mobile.isEmpty()) {
            //mobile number is blank
            responseJson.addProperty("message", "Please fill Your Mobile Number");

        } else if (!Validations.isMobileNumberValid(mobile)) {
            //invalid mobile number
            responseJson.addProperty("message", "Invalid Mobile Number");

        } else if (password.isEmpty()) {
            //password is blank
            responseJson.addProperty("message", "Please fill Your Password");

        } else if (!Validations.isPasswordValid(password)) {
            //password is invalid
            responseJson.addProperty("message", "Invalid Password");

        } else {

            Session session = HibernateUtil.getSessionFactory().openSession();

            //search mobile number
            Criteria criteria1 = session.createCriteria(User.class);
            criteria1.add(Restrictions.eq("mobile", mobile));
            criteria1.add(Restrictions.eq("password", password));
            
            if (!criteria1.list().isEmpty()) {
                //user found
                User user = (User) criteria1.uniqueResult();

                User_Status user_Status = (User_Status) session.get(User_Status.class, 1);

                user.setUser_status(user_Status);

                session.update(user);
                session.beginTransaction().commit();

                responseJson.add("user", gson.toJsonTree(user));
                responseJson.addProperty("success", true);
                responseJson.addProperty("message", "Sign In Success");

            } else {
                //user not found
                responseJson.addProperty("message", "Invalid Credentials");

            }
            session.close();
        }

        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(responseJson));
    }
}
