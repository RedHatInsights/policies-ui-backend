package com.redhat.cloud.policies.app;

import com.redhat.cloud.policies.app.model.Msg;

import javax.json.stream.JsonParsingException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Generic exception mapper to catch runtime exceptions and make us
 * return less crap to the client.
 */
@Provider
public class CustomExceptionMapper implements ExceptionMapper<RuntimeException> {
  @Override
  public Response toResponse(RuntimeException exception) {

    Response.ResponseBuilder builder;
    if (exception instanceof NotFoundException) {
      builder = Response.status(Response.Status.NOT_FOUND);
    }
    else if (exception instanceof JsonParsingException) {
      builder = Response.status(Response.Status.BAD_REQUEST);
    }
    else if (exception.getMessage().contains("RESTEASY003340") || exception.getMessage().contains("RESTEASY008200")) {
      builder = Response.status(Response.Status.BAD_REQUEST);
    } else {
      builder = Response.serverError();

      // we only print the stack on something we don't know yet
      exception.printStackTrace();
    }

    builder.type(MediaType.APPLICATION_JSON_TYPE)
        .entity(new Msg("Something went wrong, please check your request"));

    Response resp = builder.build();

    return resp;
  }
}
