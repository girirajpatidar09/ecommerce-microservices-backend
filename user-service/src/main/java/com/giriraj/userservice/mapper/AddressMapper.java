package com.giriraj.userservice.mapper;

import com.giriraj.userservice.dto.AddressDTO;
import com.giriraj.userservice.entity.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface
AddressMapper {

    @Mapping(target = "id", ignore = true)
    Address toEntity(AddressDTO dto);

    AddressDTO toDTO(Address address);
}