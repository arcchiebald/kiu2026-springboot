package com.kiu.recipes.controller.web;

import com.kiu.recipes.exception.DuplicateResourceException;
import com.kiu.recipes.exception.ResourceInUseException;
import com.kiu.recipes.service.ChefService;
import com.kiu.recipes.service.RecipeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Server-rendered pages for chefs: list, profile with their recipes, create, delete. */
@Controller
public class ChefWebController {

    private static final int PAGE_SIZE = 12;

    private final ChefService chefService;
    private final RecipeService recipeService;

    public ChefWebController(ChefService chefService, RecipeService recipeService) {
        this.chefService = chefService;
        this.recipeService = recipeService;
    }

    @GetMapping("/chefs")
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("chefs",
                chefService.findAll(PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by("name"))));
        model.addAttribute("topChefs", chefService.top(5));
        return "chef/list";
    }

    @GetMapping("/chefs/new")
    public String newForm(Model model) {
        model.addAttribute("chefForm", new ChefForm());
        return "chef/form";
    }

    @PostMapping("/chefs")
    public String create(@Valid @ModelAttribute("chefForm") ChefForm form, BindingResult result,
                         RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                chefService.create(form.toRequest());
                redirect.addFlashAttribute("successMessage", "Chef \"" + form.getName().trim() + "\" was added.");
                return "redirect:/chefs";
            } catch (DuplicateResourceException e) {
                result.rejectValue("email", "email.duplicate", e.getMessage());
            }
        }
        return "chef/form";
    }

    @GetMapping("/chefs/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("chef", chefService.findById(id));
        model.addAttribute("recipes", recipeService.findByChef(id));
        return "chef/detail";
    }

    @PostMapping("/chefs/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            chefService.delete(id);
            redirect.addFlashAttribute("successMessage", "Chef deleted.");
        } catch (ResourceInUseException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage() + " — delete or reassign them first.");
        }
        return "redirect:/chefs";
    }
}
