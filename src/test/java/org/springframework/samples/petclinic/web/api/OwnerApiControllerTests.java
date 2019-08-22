package org.springframework.samples.petclinic.web.api;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.mapper.OwnerMapper;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.OwnerDto;
import org.springframework.samples.petclinic.service.ClinicService;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OwnerApiControllerTests {

  @Autowired WebTestClient webTestClient;

  @Autowired OwnerMapper ownerMapper;

  @MockBean ClinicService clinicService;

  @Test
  public void shouldNotGetOwnerById() throws Exception {
    webTestClient
        .get()
        .uri("/api/owner/20")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectHeader()
        .contentType(MediaType.APPLICATION_JSON_UTF8)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Owner with ID '20' is unknown.");
  }

  @Test
  public void shouldGetOwnerById() throws Exception {
    when(clinicService.findOwnerById(1)).thenReturn(setupOwners().get(1));

    webTestClient
        .get()
        .uri("/api/owner/1")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentType(MediaType.APPLICATION_JSON_UTF8)
        .expectBody()
        .jsonPath("$.id")
        .isEqualTo(1)
        .jsonPath("$.city")
        .isEqualTo("Mainz")
        .jsonPath("$.lastName")
        .isEqualTo("Mueck");
  }

  @Test
  public void shouldFindOwners() throws Exception {
    final List<Owner> owners = setupOwners();
    owners.remove(1);
    when(clinicService.findOwnerByLastName("mueller")).thenReturn(owners);

    webTestClient
        .get()
        .uri("/api/owner?lastName=mueller")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentType(MediaType.APPLICATION_JSON_UTF8)
        .expectBody()
        .jsonPath("$.[0].id")
        .isEqualTo(0)
        .jsonPath("$.[1].id")
        .isEqualTo(2);
  }

  @SuppressWarnings("SameReturnValue")
  @Test
  public void shouldCreateOwner() throws Exception {

    doAnswer(
            (Answer<Void>)
                invocation -> {
                  Owner receivedOwner = (Owner) invocation.getArguments()[0];
                  receivedOwner.setId(666);
                  return null;
                })
        .when(clinicService)
        .saveOwner(any());

    final Owner newOwner = setupOwners().get(0);
    newOwner.setId(null);

    OwnerDto newOwnerDto = ownerMapper.ownerToOwnerDto(newOwner);

    ObjectMapper mapper = new ObjectMapper();
    String ownerAsJsonString = mapper.writeValueAsString(newOwnerDto);
    newOwnerDto.setId(666);
    String newOwnerAsJsonString = mapper.writeValueAsString(newOwnerDto);

    webTestClient
        .post()
        .uri("/api/owner")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .syncBody(ownerAsJsonString)
        .exchange()
        .expectStatus()
        .isCreated()
        .expectHeader()
        .contentType(MediaType.APPLICATION_JSON_UTF8)
        .expectBody()
        .jsonPath("$.id")
        .isEqualTo(666)
        .jsonPath("$.firstName")
        .isEqualTo("Klaus-Dieter")
        .jsonPath("$.lastName")
        .isEqualTo("Mueller")
        .jsonPath("$.address")
        .isEqualTo("My Street 123")
        .jsonPath("$.city")
        .isEqualTo("Hamburg")
        .jsonPath("$.telephone")
        .isEqualTo("1234567")
        .jsonPath("$.pets")
        .isArray();
  }

  @Test
  public void shouldReturnBindingErrors() throws Exception {

    final Owner newOwner = setupOwners().get(0);
    newOwner.setId(null);
    newOwner.setLastName(null);

    ObjectMapper mapper = new ObjectMapper();
    OwnerDto newOwnerDto = ownerMapper.ownerToOwnerDto(newOwner);
    String ownerAsJsonString = mapper.writeValueAsString(newOwnerDto);

    webTestClient
        .post()
        .uri("/api/owner")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .syncBody(ownerAsJsonString)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectHeader()
        .contentType(MediaType.APPLICATION_JSON_UTF8)
        .expectBody()
        .jsonPath("$.errors[0].field")
        .isEqualTo("lastName")
        .jsonPath("$.errors[0].defaultMessage")
        .isEqualTo("may not be empty");
  }

  private static List<Owner> setupOwners() {

    @SuppressWarnings("TooBroadScope")
    final List<Owner> owners = new LinkedList<>();

    Owner owner = new Owner();
    owner.setId(0);
    owner.setAddress("My Street 123");
    owner.setCity("Hamburg");
    owner.setFirstName("Klaus-Dieter");
    owner.setLastName("Mueller");
    owner.setTelephone("1234567");
    owners.add(owner);

    owner = new Owner();
    owner.setId(1);
    owner.setAddress("Hier und Da 123");
    owner.setCity("Mainz");
    owner.setFirstName("Hein");
    owner.setLastName("Mueck");
    owner.setTelephone("765432");
    owners.add(owner);

    owner = new Owner();
    owner.setId(2);
    owner.setAddress("Lange Reihe");
    owner.setCity("Hamburg");
    owner.setFirstName("Peter");
    owner.setLastName("Mueller");
    owner.setTelephone("445566");
    owners.add(owner);

    return owners;
  }
}
