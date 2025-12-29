package com.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUsernameRequest {

    @NotBlank
    @Size(min = 4, max = 50)
    private String newUsername;
    public String getNewUsername() {
        return newUsername;
    }
}
