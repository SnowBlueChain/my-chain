package api;

import controller.Controller;
import lang.Lang;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import settings.Settings;
import utils.APIUtils;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path("core")
@Produces(MediaType.APPLICATION_JSON)
public class CoreResource {
    @Context
    HttpServletRequest request;

    @GET
    @Path("/stop")
    public String stop() {
        String password = null;
        APIUtils.askAPICallAllowed(password, "GET core/stop", request);

        if (Controller.getInstance().doesWalletExists() && !Controller.getInstance().isWalletUnlocked()) {
            throw ApiErrorFactory.getInstance().createError(ApiErrorFactory.ERROR_WALLET_LOCKED);
        }

        //STOP
        Controller.getInstance().stopAll(0);
        //	System.exit(0);

        //RETURN
        return String.valueOf(true);
    }

    @GET
    @Path("/status")
    public String getStatus() {
        return String.valueOf(Controller.getInstance().getStatus());
    }

    @GET
    @Path("/status/forging")
    public String getForgingStatus() {
        return String.valueOf(Controller.getInstance().getForgingStatus().getStatuscode());
    }

    @GET
    @Path("/isuptodate")
    public String isUpToDate() {
        return String.valueOf(Controller.getInstance().checkStatus(0));
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("/settings")
    public String getSettings() {
        if (Controller.getInstance().doesWalletExists() && !Controller.getInstance().isWalletUnlocked()) {
            throw ApiErrorFactory.getInstance().createError(ApiErrorFactory.ERROR_WALLET_LOCKED);
        }

        if (!Controller.getInstance().doesWalletExists() || Controller.getInstance().isWalletUnlocked()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("settings.json", Settings.getInstance().Dump());
            jsonObject.put("peers.json", Settings.getInstance().getPeersJson());
            return jsonObject.toJSONString();
        }

        return "";
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("/version")
    public String getVersion() {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("version", Controller.getInstance().getVersion());
        jsonObject.put("buildDate", Controller.getInstance().getBuildDateString());
        jsonObject.put("buildTimeStamp", Controller.getInstance().getBuildTimestamp());


        return jsonObject.toJSONString();
    }

    @GET
    @Path("/notranslate")
    public String getNoTranslate() {
        return JSONValue.toJSONString(Lang.getInstance().getNoTranslate());
    }

}
