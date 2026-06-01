package com.nhom18.flashlock.data.remote.dictionary;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface DictionaryApi {
    @GET("entries/en/{word}")
    Call<List<DictionaryResponse>> getDefinition(@Path("word") String word);

    @GET("https://api.datamuse.com/words")
    Call<List<SuggestionResponse>> getSuggestions(
            @Query("sp") String query,
            @Query("md") String metadata,
            @Query("max") int limit
    );
}