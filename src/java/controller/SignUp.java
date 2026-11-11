package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import entity.User;
import entity.User_Status;
import java.io.File;
import java.io.IOException;
//import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import model.HibernateUtil;
import model.Validations;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

@MultipartConfig
@WebServlet(name = "SignUp", urlPatterns = {"/SignUp"})
public class SignUp extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        Gson gson = new Gson();
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("success", false);

//        JsonObject requestJson = gson.fromJson(req.getReader(), JsonObject.class);
        String mobile = req.getParameter("mobile");
        String firstName = req.getParameter("firstName");
        String lastName = req.getParameter("lastName");
        String password = req.getParameter("password");
        Part avaratImage = req.getPart("avatarImage");
        
        if (mobile.isEmpty()) {
            //mobile number is blank
            responseJson.addProperty("message", "Please fill Your Mobile Number");

        } else if (!Validations.isMobileNumberValid(mobile)) {
            //invalid mobile number
            responseJson.addProperty("message", "Invalid Mobile Number");

        } else if (firstName.isEmpty()){
            //first name is blank
            responseJson.addProperty("message", "Please fill Your First Name");

        } else if (lastName.isEmpty()){
            //last name is blank
            responseJson.addProperty("message", "Please fill Your Last Name");

        } else if (password.isEmpty()){
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
            
            if (!criteria1.list().isEmpty()) {
                //mobile number found
                responseJson.addProperty("message", "Mobile number already used");
                
            } else {
                //mobile number not found
                
                User user = new User();
                user.setMobile(mobile);
                user.setFirst_name(firstName);
                user.setLast_name(lastName);
                user.setPassword(password);
                user.setRegistered_date_time(new Date());

                //get user status 2 = offline
                User_Status user_Status = (User_Status) session.get(User_Status.class, 2);
                user.setUser_status(user_Status);

                session.save(user);
                session.beginTransaction().commit();

                //check uploaded Image
                if (avaratImage != null) {
                    //Image Selected
                    String serverPath = req.getServletContext().getRealPath("");
                    String avatarImagePath = serverPath + File.separator + "AvatarImages" + File.separator + mobile + ".png";
//                System.out.println(avatarImagePath);
                    File file = new File(avatarImagePath);
                    Files.copy(avaratImage.getInputStream(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                responseJson.addProperty("success", true);
                responseJson.addProperty("message", "Registration Completed!");
                session.close();
            }
        }

        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(responseJson));

    }
}
