package ro.cnpr.inventar.network;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import ro.cnpr.inventar.model.AssetDto;
import ro.cnpr.inventar.model.AssetUpdateRequest;
import ro.cnpr.inventar.model.HealthResponse;
import ro.cnpr.inventar.model.RoomDto;

public interface ApiService {

    @GET("health")
    Call<HealthResponse> getHealth();

    @GET("rooms")
    Call<List<RoomDto>> getRooms();

    @GET("rooms/{roomId}/assets")
    Call<List<AssetDto>> getAssetsInRoom(@Path("roomId") long roomId);

    @GET("assets/by-nr/{nrInventar}")
    Call<AssetDto> searchAssets(@Path("nrInventar") String nrInventar);

    @PATCH("assets/by-nr/{nrInventar}")
    Call<AssetDto> updateAssetByNr(
            @Path("nrInventar") String nrInventar,
            @Body AssetUpdateRequest request
    );

    @DELETE("assets/by-nr/{nrInventar}")
    Call<AssetDto> deleteAssetByNr(
            @Path("nrInventar") String nrInventar
    );
}
