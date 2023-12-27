package dev.slangware.ultralight.example;

import dev.slangware.ultralight.HtmlScreen;
import dev.slangware.ultralight.ViewController;
import dev.slangware.ultralight.annotations.HTMLRoute;
import lombok.SneakyThrows;
import net.minecraft.client.Minecraft;
import spark.Request;
import spark.Response;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

public class ExampleScreen extends HtmlScreen {
    private final static ThymeleafTemplateEngine templateEngine = new ThymeleafTemplateEngine();

    public ExampleScreen(ViewController viewController) {
        super(viewController);
    }

    /**
     * Handles the request and returns the rendered HTML page.
     *
     * @param request The HTTP request object.
     * @param response The HTTP response object.
     * @return The rendered HTML page.
     */
    @HTMLRoute
    public String handleRequest(Request request, Response response) {
        // Build the model with the version parameter, you can use it with th:text="${version}" please check out thymeleaf tutorial
        // Already USER_UUID, USERNAME exists in the model and can be used in thymeleaf
        ModelBuilder build = ModelBuilder.builder()
                .append("version", "1.0-SNAPSHOT")
                .append("username", Minecraft.getMinecraft().getSession().getUsername())
                .append("uuid", Minecraft.getMinecraft().getSession().getPlayerID());


        // Render the HTML page using the template engine and the model, the model can be anything supported by thymeleaf
        return templateEngine.render(templateEngine.modelAndView(build.build(), "main")); // main.html
    }

    /**
     * Handles the action request.
     *
     * @param request  The request object.
     * @param response The response object.
     */
    @SneakyThrows
    @HTMLRoute(path = "/action", method = "POST")
    public void handleActionRequest(Request request, Response response) {
        // Get the type parameter from the request
        String type = request.queryParams("type");

        // Handle the different types of actions
        switch (type) {
            case "print":
                // Print the message parameter to the console
                System.out.println(request.queryParams("message"));
                break;
            case "close":
                // Close the GUI screen in Minecraft
                // Need to call with addScheduledTask because this method is not on the main thread
                this.mc.addScheduledTask(() ->{
                   mc.displayGuiScreen(null);
                });
                break;
        }

        // Redirect the user to the home page
        response.redirect("/", 307 /* temporary redirect */);
    }

}
