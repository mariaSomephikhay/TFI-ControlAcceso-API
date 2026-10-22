package com.unla.gestionUsuario;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.unla.gestionUsuario.controller.UserController;
import com.unla.gestionUsuario.entities.User;
import com.unla.gestionUsuario.exceptions.UserException;
import com.unla.gestionUsuario.mapper.IUserMapper;
import com.unla.gestionUsuario.services.implementations.UserService;

class UserControllerTests {
    @Test
    void duplicateUserReturnsConflict() throws Exception {
        UserService service = org.mockito.Mockito.mock(UserService.class);
        IUserMapper mapper = org.mockito.Mockito.mock(IUserMapper.class);
        UserController controller = new UserController();
        ReflectionTestUtils.setField(controller, "userService", service);
        ReflectionTestUtils.setField(controller, "userMapper", mapper);
        when(mapper.dtoToUser(any())).thenReturn(new User());
        when(service.createUser(any())).thenThrow(UserException.of(UserException.Type.USER_ALREADY_EXISTS));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        mvc.perform(post("/users/new")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"test-user\",\"password\":\"new-password\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().string("Error al crear el usuario, El usuario ya existe."));
    }
}
