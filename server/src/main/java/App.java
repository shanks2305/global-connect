import com.mongodb.BasicDBObject;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static spark.Spark.*;

public class App {

    public static void main(String[] args) {

        port(8000);
        enableCORS();

        String uri = "mongodb://localhost:27017/globalconnect";

        // Welcome Message to API
        get("/", (req, res) -> {
            return "Welcome to Spark Server Api for Global Connect";
        });
        // User sign-up
        post("/api/user/signup",(req,res) ->{
            String name = req.queryParams("name");
            String email = req.queryParams("email");
            String password = req.queryParams("password");
            String phone = req.queryParams("phone");
            String address = req.queryParams("address");

            try(MongoClient mongoClient = MongoClients.create(uri)){
                MongoDatabase userdatabase = mongoClient.getDatabase("globalconnect");
                MongoCollection<Document> userCollection = userdatabase.getCollection("users");
                try {
                    Document document = userCollection.find(new Document("email",email)).first();
                    res.status(400);
                    res.type("application/json");
                    System.out.println(document.toString());
                    return "{\"error\":\"Already a user\"}";
                }catch (Exception e){
                    System.out.println(e);
                }
                    Document userdata = new Document();
                    userdata.append("name",name);
                    userdata.append("email",email);
                    userdata.append("password",password);
                    userdata.append("phone",phone);
                    userdata.append("address",address);
                    userdata.append("type",1);

                    userCollection.insertOne(userdata);
                    res.status(200);
                    res.type("application/json");
                    return userdata.toJson();

            }
        });
        //End of sign-up
        // User sign-in
        post("/api/user/signin",((request, response) ->{
            System.out.println(request);
            String email = request.queryParams("email");
            String password = request.queryParams("password");
            try(MongoClient mongoClient = MongoClients.create(uri)){
                MongoDatabase userDatabase = mongoClient.getDatabase("globalconnect");
                MongoCollection<Document> userCollection = userDatabase.getCollection("users");
                try{
                    Document userdata = userCollection.find(new Document("email",email)).first();
                    if(userdata.get("password").equals(password)){
                        String uid = userdata.get("_id").toString();
                        response.cookie("uid",uid,1000*60*60*24*30*12);
                        response.status(200);
                        response.type("application/json");
                        return userdata.toJson();
                    }else{
                        response.status(400);
                        response.type("application/json");
                        return "{\"error\":\"Input password was incorrect\"}";
                    }
                }catch (NullPointerException nullPointerException){
                    response.status(400);
                    response.type("application/json");
                    return "{\"error\":\"No user found\"}";
                }
            }
        }));
        //End of Sign-in module
        //Add user profile pic
        post("/api/user/add-profile-pic/:uid",((request, response) -> {
            String uid = request.params("uid");
            String image = request.queryParams("image");
            try (MongoClient mongoClient = MongoClients.create(uri)) {
                MongoDatabase userDatabase = mongoClient.getDatabase("globalconnect");
                MongoCollection<Document> userCollection = userDatabase.getCollection("users");

                try {
                    userCollection.updateOne(
                            Filters.eq("_id", new ObjectId(uid)),
                            Updates.set("image", image)
                    );
                    response.status(200);
                    response.type("application/json");
                    return "{\"Message\":\"User created\"}";
                } catch (Exception e) {
                    response.status(400);
                    response.type("application/json");
                    return "{\"error\":\"No user found\"}";
                }
            }
        }));

        //All user

        get("/api/alluser/",(request, response) -> {
             try(MongoClient mongoClient = MongoClients.create(uri)) {
                MongoDatabase userdatabase = mongoClient.getDatabase("globalconnect");
                MongoCollection<Document> userCollection = userdatabase.getCollection("users");
                try {
                    List<String> userData = new ArrayList<>();
                    MongoCursor<Document> cursor = userCollection.find().iterator();
                    while (cursor.hasNext()){
                        userData.add(cursor.next().toJson());
                    }
                    response.type("application/json");
                    response.status(200);
                    return "[" + String.join(", ", userData) + "]";
                }catch (Exception e){
                    response.type("application/json");
                    response.status(400);
                    return "{\"error\":\"Error while loading user\"}";
                }
            }
        });
        //End all user

    //Delete User
        delete("/api/user/:uid",(request, response) -> {
            String uid = request.params("uid");
            try(MongoClient mongoClient = MongoClients.create(uri)) {
                MongoDatabase userdatabase = mongoClient.getDatabase("globalconnect");
                MongoCollection<Document> userCollection = userdatabase.getCollection("posts");
                try {
                    userCollection.deleteOne(
                            Filters.eq("_id",uid)
                    );
                    response.type("application/json");
                    response.status(200);
                    return "{\"message\":\"User deleted\"}";
                }catch (Exception e){
                    response.type("application/json");
                    response.status(400);
                    return "{\"error\":\"Error while loading user\"}";
                }
            }
        });
        //End Delete User

        // Get user data
        get("/api/user/:uid",((request, response) ->{
            String uid = request.params("uid");
            try(MongoClient mongoClient = MongoClients.create(uri)){
                MongoDatabase userDatabase = mongoClient.getDatabase("globalconnect");
                MongoCollection<Document> userCollection = userDatabase.getCollection("users");

                try {
                    Document userdata = userCollection.find(new Document("_id",new ObjectId(uid))).first();
                    response.type("application/json");
                    response.status(200);
                    return userdata.toJson();
                }catch (NullPointerException nullPointerException){
                    response.type("application/json");
                    response.status(400);
                    return "{\"error\":\"No user found\"}";
                }
            }
        }));
        //End of user data module
        //Create a topic
        post("/api/post/createpost/:uid",((request, response) -> {

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();

            String title = request.queryParams("title");
            String desc = request.queryParams("desc");
            String image = request.queryParams("image");
            String location = request.queryParams("location");
            String uid = request.params("uid");
            try(MongoClient mongoClient = MongoClients.create(uri)) {
                MongoDatabase userdatabase = mongoClient.getDatabase("globalconnect");
                MongoCollection<Document> postCollection = userdatabase.getCollection("posts");
                try {
                    Document postData = new Document();
                    postData.append("uid",new ObjectId(uid));
                    postData.append("title",title);
                    postData.append("desc",desc);
                    postData.append("image",image);
                    postData.append("location",location);
                    postData.append("votes",0);
                    postData.append("timestamp",dtf.format(now));
                    postCollection.insertOne(postData);
                    response.type("application/json");
                    response.status(200);
                    return postData.toJson();
                }catch (Exception e){
                    response.type("application/json");
                    response.status(400);
                    return "{\"error\":\"Cannot add a post\"}";
                }
            }
        }));
        //End of create topic module
        //Get all topic
        get("/api/post/posts",((request, response) -> {
            try(MongoClient mongoClient = MongoClients.create(uri)) {
                MongoDatabase userdatabase = mongoClient.getDatabase("globalconnect");
                MongoCollection<Document> postCollection = userdatabase.getCollection("posts");
                try {
                    List<String> postData = new ArrayList<>();
                    MongoCursor<Document> cursor = postCollection.find().iterator();
                    while (cursor.hasNext()){
                        postData.add(cursor.next().toJson());
                    }
                    response.type("application/json");
                    response.status(200);
                    return "[" + String.join(", ", postData) + "]";
                }catch (NullPointerException nullPointerException){
                    response.type("application/json");
                    response.status(400);
                    return "{\"error\":\"Error while loading Posts\"}";
                }
            }
        }));
        //End of get all topics
        //Delete a post
        delete("/api/post/:pid",(request, response) -> {
            String pid = request.params("pid");
            try(MongoClient mongoClient = MongoClients.create(uri)) {
                MongoDatabase userdatabase = mongoClient.getDatabase("globalconnect");
                MongoCollection<Document> postCollection = userdatabase.getCollection("posts");
                try {
                    postCollection.deleteOne(
                      Filters.eq("_id",pid)
                    );
                    response.type("application/json");
                    response.status(200);
                    return "{\"message\":\"Post deleted\"}";
                }catch (Exception e){
                    response.type("application/json");
                    response.status(400);
                    return "{\"error\":\"Error while loading Post\"}";
                }
            }
        });
        //end of delete post
        //Get a particular post
        get("/api/post/:pid",((request, response) -> {
            String pid = request.params("pid");
            try(MongoClient mongoClient = MongoClients.create(uri)) {
                MongoDatabase userdatabase = mongoClient.getDatabase("globalconnect");
                MongoCollection<Document> postCollection = userdatabase.getCollection("posts");
                try {
                    Document postData = postCollection.find(new Document("_id",new ObjectId(pid))).first();
                    response.type("application/json");
                    response.status(200);
                    return postData.toJson();
                }catch (Exception e){
                    response.type("application/json");
                    response.status(400);
                    return "{\"error\":\"Error while loading Post\"}";
                }
            }
        }));
        //end of get a particular post
        //Logged in User posts
        get("/api/post/user/posts/:uid",((request, response) -> {
            // String uid = request.cookie("uid");
            String uid = request.params("uid");
            try(MongoClient mongoClient = MongoClients.create(uri)) {
                MongoDatabase userdatabase = mongoClient.getDatabase("globalconnect");
                MongoCollection<Document> postCollection = userdatabase.getCollection("posts");
                try {
                    List<String> postData = new ArrayList<>();
                    MongoCursor<Document> cursor = postCollection.find(new BasicDBObject("uid",new ObjectId(uid))).iterator();
                    while (cursor.hasNext()){
                        postData.add(cursor.next().toJson());
                    }
                    response.type("application/json");
                    response.status(200);
                    return "[" + String.join(", ", postData) + "]";
                }catch (NullPointerException nullPointerException){
                    response.type("application/json");
                    response.status(400);
                    return "{\"error\":\"Error while loading Posts\"}";
                }
            }
        }));
        //End of logged in user post module
        //Add a comment to a post
        post("/api/post/:pid/addComment",(request, response) -> {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();

            String pid = request.params("pid");
            String comment = request.queryParams("comment");
            String uid = request.queryParams("uid");
            String name = request.queryParams("name");

            try(MongoClient mongoClient = MongoClients.create(uri)){
                MongoDatabase userdatabase = mongoClient.getDatabase("globalconnect");
                MongoCollection<Document> postComments = userdatabase.getCollection("post-comments");
                try {
                    Document commentData = new Document();

                    commentData.append("uid",new ObjectId(uid));
                    commentData.append("pid",new ObjectId(pid));
                    commentData.append("username",name);
                    commentData.append("comment",comment);
                    commentData.append("timestamp",dtf.format(now));

                    postComments.insertOne(commentData);
                    response.type("application/json");
                    response.status(200);
                    return "{\"message\":\"Comment added\"}";
                }catch (Exception e){
                    response.type("application/json");
                    response.status(400);
                    return "{\"error\":\"Error while creating a comment\"}";
                }
            }

        });
        //End of add comment post
        //Get comments to a post
        get("/api/post/comment/:pid",(request, response) -> {
            String pid = request.params("pid");
            try(MongoClient mongoClient = MongoClients.create(uri)) {
                MongoDatabase userdatabase = mongoClient.getDatabase("globalconnect");
                MongoCollection<Document> postComments = userdatabase.getCollection("post-comments");
                try {
                    List<String> commentsData = new ArrayList<>();
                    MongoCursor<Document> cursor = postComments.find(new BasicDBObject("pid",new ObjectId(pid))).iterator();
                    while (cursor.hasNext()){
                        commentsData.add(cursor.next().toJson());
                    }
                    response.type("application/json");
                    response.status(200);
                    return "[" + String.join(", ", commentsData) + "]";
                }catch (NullPointerException e){
                    response.type("application/json");
                    response.status(400);
                    return "{\"error\":\"Error while getting a comment\"}";
                }
            }
        });

        put("/api/post/upvote/:pid",(request, response) -> {
            String pid = request.params("pid");
            try(MongoClient mongoClient = MongoClients.create(uri)) {
                MongoDatabase userdatabase = mongoClient.getDatabase("globalconnect");
                MongoCollection<Document> postCollection = userdatabase.getCollection("posts");
                try {
                    postCollection.updateOne(
                            Filters.eq("_id",new ObjectId(pid)),
                            Updates.inc("votes",1)
                    );
                    response.type("application/json");
                    response.status(200);
                    return "{\"message\":\"Upvote success\"}";
                }catch (Exception e){
                    response.type("application/json");
                    response.status(400);
                    return "{\"error\":\"Error while up vote\"}";
                }
            }
        });
        put("/api/post/downvote/:pid",(request, response) -> {
            String pid = request.params("pid");
            try(MongoClient mongoClient = MongoClients.create(uri)) {
                MongoDatabase userdatabase = mongoClient.getDatabase("globalconnect");
                MongoCollection<Document> postCollection = userdatabase.getCollection("posts");
                try {
                    postCollection.updateOne(
                            Filters.eq("_id",new ObjectId(pid)),
                            Updates.inc("votes",-1)
                    );
                    response.type("application/json");
                    response.status(200);
                    return "{\"message\":\"Upvote success\"}";
                }catch (Exception e){
                    response.type("application/json");
                    response.status(400);
                    return "{\"error\":\"Error while down vote\"}";
                }
            }
        });

    }
    private static void enableCORS() {

        options("/*", (request, response) -> {

            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Headers", "*");
        });
    }
}
