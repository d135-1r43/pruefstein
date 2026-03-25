package com.pruefstein.dashboard.api;

import com.pruefstein.user.domain.User;
import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.Path;

public class Dashboard extends Controller {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index(long userCount);
    }

    @Path("/")
    public TemplateInstance index() {
        return Templates.index(User.count());
    }
}
