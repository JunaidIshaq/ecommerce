package com.shopfast.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    public String id;

    @JsonProperty("name")
    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    public String name;

    @JsonProperty("description")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    public String description;

    @JsonProperty("parent_id")
    public String parentId;

    @JsonProperty("subcategory_ids")
    public List<String> subCategoryIds;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("created_by")
    private String createdBy;

    @JsonProperty("updated_by")
    private String updatedBy;

}
