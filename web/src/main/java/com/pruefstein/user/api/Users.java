package com.pruefstein.user.api;

import com.pruefstein.user.domain.User;
import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.POST;
import org.jboss.resteasy.reactive.RestForm;

import java.util.List;

public class Users extends Controller {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index(List<User> users);
    }

    public TemplateInstance index() {
        return Templates.index(User.listAll());
    }

    @POST
    @Transactional
    public void create(
            @RestForm @NotBlank String firstname,
            @RestForm @NotBlank String lastname,
            @RestForm @Email @NotBlank String mail) {
        if (validationFailed()) {
            index();
            return;
        }
        User user = new User();
        user.setFirstname(firstname);
        user.setLastname(lastname);
        user.setMail(mail);
        user.persist();
        index();
    }

    @POST
    @Transactional
    public void update(
            @RestForm Long id,
            @RestForm @NotBlank String firstname,
            @RestForm @NotBlank String lastname,
            @RestForm @Email @NotBlank String mail) {
        if (validationFailed()) {
            index();
            return;
        }
        User user = User.findById(id);
        if (user == null) {
            notFound();
            return;
        }
        user.setFirstname(firstname);
        user.setLastname(lastname);
        user.setMail(mail);
        index();
    }

    @POST
    @Transactional
    public void delete(@RestForm Long id) {
        User.deleteById(id);
        index();
    }
}
