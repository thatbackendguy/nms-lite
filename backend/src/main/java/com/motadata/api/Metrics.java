package com.motadata.api;

import com.motadata.Bootstrap;
import com.motadata.utils.Utils;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import static com.motadata.api.APIServer.LOGGER;
import static com.motadata.contants.Constants.*;

public class Metrics
{
    private final EventBus eventBus;

    private final Vertx vertx;

    private final Router subRouter;

    public Metrics()
    {
        this.vertx = Bootstrap.getVertx();

        this.eventBus = Bootstrap.getVertx().eventBus();

        this.subRouter = Router.router(vertx);
    }

    public void init(Router router)
    {
        router.route("/metrics/*").subRouter(subRouter);

        subRouter.route(HttpMethod.GET, URL_SEPARATOR + "data").handler(this::getMetricsData);

        subRouter.route(HttpMethod.GET, URL_SEPARATOR + "interfaces").handler(this::getDeviceInterfacesData);
    }

    public void getMetricsData(RoutingContext routingContext)
    {
        LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

        routingContext.request().bodyHandler(buffer -> {

            var requestObject = buffer.toJsonObject().put(TABLE_NAME, NETWORK_INTERFACE).put(EVENT_NAME, GET_INTERFACE_METRICS);

            if(Utils.validateRequestBody(requestObject))
            {
                eventBus.request(GET_EVENT, requestObject, messageAsyncResult -> {

                    if(messageAsyncResult.succeeded())
                    {
                        routingContext.json(new JsonObject(messageAsyncResult.result().body().toString()).put(STATUS, SUCCESS));
                    }
                    else
                    {
                        routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error fetching data").put(ERR_MESSAGE, messageAsyncResult.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())).toString());
                    }
                });
            }
            else
            {
                routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error fetching data").put(ERR_MESSAGE, INVALID_REQUEST_BODY).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())).toString());
            }

        });
    }

    public void getDeviceInterfacesData(RoutingContext routingContext)
    {
        LOGGER.info(REQ_CONTAINER, routingContext.request().method(), routingContext.request().path(), routingContext.request().remoteAddress());

        routingContext.request().bodyHandler(buffer -> {

            var requestObject = buffer.toJsonObject().put(TABLE_NAME, NETWORK_INTERFACE).put(EVENT_NAME, GET_INTERFACES);

            if(Utils.validateRequestBody(requestObject))
            {
                eventBus.request(GET_EVENT, requestObject, messageAsyncResult -> {

                    if(messageAsyncResult.succeeded())
                    {
                        routingContext.json(new JsonObject(messageAsyncResult.result().body().toString()).put(STATUS, SUCCESS));
                    }
                    else
                    {
                        routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error fetching data").put(ERR_MESSAGE, messageAsyncResult.cause().getMessage()).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())).toString());
                    }
                });
            }
            else
            {
                routingContext.response().setStatusCode(HttpResponseStatus.BAD_REQUEST.code()).putHeader(CONTENT_TYPE, APP_JSON).end(new JsonObject().put(STATUS, FAILED).put(ERROR, new JsonObject().put(ERROR, "Error fetching data").put(ERR_MESSAGE, INVALID_REQUEST_BODY).put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())).toString());
            }

        });
    }
}
