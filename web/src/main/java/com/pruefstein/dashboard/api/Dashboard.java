package com.pruefstein.dashboard.api;

import com.pruefstein.user.repository.UserRepository;
import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

public class Dashboard extends Controller {

    @Inject
    UserRepository userRepository;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index(long userCount);
    }

    @Path("/")
    public TemplateInstance index() {
        return Templates.index(userRepository.count());
    }
}
