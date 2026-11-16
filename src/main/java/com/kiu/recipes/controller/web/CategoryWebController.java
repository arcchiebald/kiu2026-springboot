package com.kiu.recipes.controller.web;

import com.kiu.recipes.exception.DuplicateResourceException;
import com.kiu.recipes.exception.ResourceInUseException;
import com.kiu.recipes.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Server-rendered page listing categories, with an inline "add" form and delete buttons. */
@Controller
public class CategoryWebController {

    private final CategoryService categoryService;

    public CategoryWebController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/categories")
    public String list(Model model) {
        model.addAttribute("categoryForm", new CategoryForm());
        model.addAttribute("categories", categoryService.findAll());
        return "category/list";
    }

    @PostMapping("/categories")
    public String create(@Valid @ModelAttribute("categoryForm") CategoryForm form, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                categoryService.create(form.toRequest());
                redirect.addFlashAttribute("successMessage", "Category \"" + form.getName().trim() + "\" was added.");
                return "redirect:/categories";
            } catch (DuplicateResourceException e) {
                result.rejectValue("name", "name.duplicate", e.getMessage());
            }
        }
        model.addAttribute("categories", categoryService.findAll());
        return "category/list";
    }

    @PostMapping("/categories/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            categoryService.delete(id);
            redirect.addFlashAttribute("successMessage", "Category deleted.");
        } catch (ResourceInUseException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage() + " — delete or move them first.");
        }
        return "redirect:/categories";
    }
}
