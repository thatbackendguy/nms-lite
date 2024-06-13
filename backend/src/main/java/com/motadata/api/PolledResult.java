package com.motadata.api;

import com.motadata.Bootstrap;
import com.motadata.constants.Constants;
import com.motadata.utils.Utils;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import static com.motadata.constants.Constants.*;
import static com.motadata.api.APIServer.LOGGER;

public class PolledResult
{

    private final Vertx vertx;

    private final Router polledMetricsSubRouter;

    public PolledResult()
    {

        this.vertx = Bootstrap.getVertx();

        this.polledMetricsSubRouter = Router.router(vertx);
    }

    public void init(Router router)
    {

        router.route("/metrics/*").subRouter(polledMetricsSubRouter);

        polledMetricsSubRouter.route(HttpMethod.GET, URL_SEPARATOR + RESULT).handler(this::getPolledMetricsData);

        polledMetricsSubRouter.route(HttpMethod.GET, URL_SEPARATOR + INTERFACE).handler(this::getInterfaces);
    }

    private void getPolledMetricsData(RoutingContext routingContext)
    {

        try
        {
            LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request()
                    .path(), routingContext.request().remoteAddress());

            routingContext.request().bodyHandler(buffer ->
            {

                var request = buffer.toJsonObject();

                if (request.containsKey(OBJECT_IP) && request.containsKey(INTERFACE_NAME) && request.containsKey(LAST))
                {
                    if (Utils.validateRequestBody(request))
                    {
                        vertx.fileSystem()
                                .readFile("./metrics-result/" + request.getString(OBJECT_IP) + "/" + request.getString(INTERFACE_NAME), asyncHandler ->
                                {
                                    if (asyncHandler.succeeded())
                                    {
                                        var records = asyncHandler.result().toString().split("\n");

                                        var response = new JsonArray();

                                        var noOfEntries = records.length;

                                        if (noOfEntries <= request.getInteger(LAST))
                                        {

                                            for (int entry = noOfEntries - 1; entry >= 0; entry--)
                                            {
                                                response.add(new JsonObject(records[ entry ]));
                                            }
                                        }
                                        else
                                        {

                                            for (int entry = noOfEntries - 1; entry >= noOfEntries - request.getInteger(LAST); entry--)
                                            {
                                                response.add(new JsonObject(records[ entry ]));
                                            }
                                        }

                                        routingContext.response()
                                                .putHeader(CONTENT_TYPE, APP_JSON)
                                                .end(new JsonObject().put(STATUS, SUCCESS)
                                                        .put(RESULT, response)
                                                        .toString());
                                    }
                                    else
                                    {
                                        routingContext.response()
                                                .putHeader(CONTENT_TYPE, APP_JSON)
                                                .end(new JsonObject().put(STATUS, FAILED)
                                                        .put(ERROR, "Error in fetching data")
                                                        .put(ERR_MESSAGE, "No data available")
                                                        .put(ERR_STATUS_CODE, HttpResponseStatus.NOT_FOUND.code())
                                                        .toString());
                                    }
                                });

                    }
                    else
                    {
                        routingContext.response()
                                .putHeader(CONTENT_TYPE, APP_JSON)
                                .end(new JsonObject().put(STATUS, FAILED)
                                        .put(ERROR, "Error in fetching data")
                                        .put(ERR_MESSAGE, "Empty values are not allowed")
                                        .put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())
                                        .toString());
                    }
                }
                else
                {
                    routingContext.response()
                            .putHeader(CONTENT_TYPE, APP_JSON)
                            .end(new JsonObject().put(STATUS, FAILED)
                                    .put(ERROR, "Error fetching data")
                                    .put(ERR_MESSAGE, INVALID_REQUEST_BODY)
                                    .put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())
                                    .toString());
                }

            });
        }
        catch (Exception exception)
        {
            routingContext.response()
                    .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                    .putHeader(CONTENT_TYPE, APP_JSON)
                    .end(new JsonObject().put(STATUS, FAILED)
                            .put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                            .put(ERROR, HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase())
                            .put(ERR_MESSAGE, exception.getMessage())
                            .toString());

            LOGGER.error(Constants.ERROR_CONTAINER, exception.getMessage());
        }
    }

    private void getInterfaces(RoutingContext routingContext)
    {

        try
        {
            LOGGER.trace(REQ_CONTAINER, routingContext.request().method(), routingContext.request()
                    .path(), routingContext.request().remoteAddress());

            routingContext.request().bodyHandler(buffer ->
            {

                var request = buffer.toJsonObject();

                if (request.containsKey(OBJECT_IP))
                {
                    if (Utils.validateRequestBody(request))
                    {
                        vertx.fileSystem().readDir("./metrics-result/" + request.getString(OBJECT_IP), result ->
                        {
                            if (result.succeeded())
                            {
                                var interfaces = new JsonArray();

                                for (var name : result.result())
                                {
                                    interfaces.add(name.substring(name.lastIndexOf("/") + 1));
                                }

                                routingContext.response()
                                        .putHeader(CONTENT_TYPE, APP_JSON)
                                        .end(new JsonObject().put(STATUS, SUCCESS).put(RESULT, interfaces).toString());

                            }
                            else
                            {
                                routingContext.response()
                                        .putHeader(CONTENT_TYPE, APP_JSON)
                                        .end(new JsonObject().put(STATUS, FAILED)
                                                .put(ERROR, "Error in fetching interfaces")
                                                .put(ERR_MESSAGE, "No interfaces found for " + request.getString(OBJECT_IP))
                                                .put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())
                                                .toString());
                            }

                        });
                    }
                    else
                    {
                        routingContext.response()
                                .putHeader(CONTENT_TYPE, APP_JSON)
                                .end(new JsonObject().put(STATUS, FAILED)
                                        .put(ERROR, "Error in fetching interfaces")
                                        .put(ERR_MESSAGE, "Empty values are not allowed")
                                        .put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())
                                        .toString());
                    }
                }
                else
                {
                    routingContext.response()
                            .putHeader(CONTENT_TYPE, APP_JSON)
                            .end(new JsonObject().put(STATUS, FAILED)
                                    .put(ERROR, "Error fetching interfaces")
                                    .put(ERR_MESSAGE, INVALID_REQUEST_BODY)
                                    .put(ERR_STATUS_CODE, HttpResponseStatus.BAD_REQUEST.code())
                                    .toString());
                }
            });
        }
        catch (Exception exception)
        {
            routingContext.response()
                    .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                    .putHeader(CONTENT_TYPE, APP_JSON)
                    .end(new JsonObject().put(STATUS, FAILED)
                            .put(ERR_STATUS_CODE, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                            .put(ERROR, HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase())
                            .put(ERR_MESSAGE, exception.getMessage())
                            .toString());

            LOGGER.error(Constants.ERROR_CONTAINER, exception.getMessage());
        }

    }

}
