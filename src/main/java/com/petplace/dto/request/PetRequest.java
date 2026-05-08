package com.petplace.dto.request;
import lombok.Data;
import java.time.LocalDate;
@Data public class PetRequest { private String name, breed, imageUrl; private LocalDate birth; }
