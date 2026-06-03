package com.nhom18.flashlock.data.repository;

import com.nhom18.flashlock.data.remote.dictionary.DictionaryApi;
import com.nhom18.flashlock.data.remote.dictionary.DictionaryResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DictionaryRepository {

    private static final String BASE_URL = "https://api.dictionaryapi.dev/api/v2/";
    private final DictionaryApi api;

    public DictionaryRepository() {
        // Khởi tạo Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(DictionaryApi.class);
    }

    public void searchOnline(String word, DictionaryCallback callback) {
        api.getDefinition(word).enqueue(new Callback<List<DictionaryResponse>>() {
            @Override
            public void onResponse(Call<List<DictionaryResponse>> call, Response<List<DictionaryResponse>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    callback.onSuccess(response.body().get(0));
                } else {
                    callback.onError("Word not found");
                }
            }

            @Override
            public void onFailure(Call<List<DictionaryResponse>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public interface DictionaryCallback {
        void onSuccess(DictionaryResponse result);
        void onError(String error);
    }

    public void getSuggestions(String query, int limit, SuggestionCallback callback) {
        api.getSuggestions(query + "*", "d", limit).enqueue(new Callback<List<com.nhom18.flashlock.data.remote.dictionary.SuggestionResponse>>() {
            @Override
            public void onResponse(Call<List<com.nhom18.flashlock.data.remote.dictionary.SuggestionResponse>> call, Response<List<com.nhom18.flashlock.data.remote.dictionary.SuggestionResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("No suggestions found");
                }
            }

            @Override
            public void onFailure(Call<List<com.nhom18.flashlock.data.remote.dictionary.SuggestionResponse>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public interface SuggestionCallback {
        void onSuccess(List<com.nhom18.flashlock.data.remote.dictionary.SuggestionResponse> result);
        void onError(String error);
    }
}