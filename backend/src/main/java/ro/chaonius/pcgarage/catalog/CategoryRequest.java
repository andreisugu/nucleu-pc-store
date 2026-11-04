package ro.chaonius.pcgarage.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CategoryRequest(@NotBlank @Size(max = 100) String name) {
}
