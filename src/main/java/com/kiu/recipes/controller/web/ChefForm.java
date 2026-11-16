package com.kiu.recipes.controller.web;

import com.kiu.recipes.dto.ChefRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Mutable form bean for creating a chef (mirrors {@link ChefRequest}). */
public class ChefForm {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 150, message = "Email must be at most 150 characters")
    private String email;

    @Size(max = 500, message = "Bio must be at most 500 characters")
    private String bio;

    public ChefRequest toRequest() {
        return new ChefRequest(name, email, bio);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
