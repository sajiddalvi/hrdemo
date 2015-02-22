package com.tekdi.hrdemo.backend;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.NotFoundException;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.datastore.Text;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.cmd.Query;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Named;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * WARNING: This generated code is intended as a sample or starting point for using a
 * Google Cloud Endpoints RESTful API with an Objectify entity. It provides no data access
 * restrictions and no data validation.
 * <p/>
 * DO NOT deploy this code unchanged as part of a real application to real users.
 */
@Api(
        name = "sensorDataApi",
        version = "v1",
        resource = "sensorData",
        namespace = @ApiNamespace(
                ownerDomain = "backend.hrdemo.tekdi.com",
                ownerName = "backend.hrdemo.tekdi.com",
                packagePath = ""
        )
)
public class SensorDataEndpoint {

    enum chunk_type {CHUNK_START, CHUNK_CONT, CHUNK_END, CHUNK_ALL_IN_ONE};

    // Data object to be sent from EKG capture device to analysis application
    private class ekg_source_data {
        int type;    // indicates start, middle, or end of stream, or all-in-one chunk
        int chunk_seq_num;       // chunk sequence number (don't care for all-in-one)
        int fs;                  // sampling frequency of raw EKG data
        int nbits;               // number of bits used for each sample; default = 24 bits in 32 bit int (MSB)
        int len;                 // length of individual chunk
        int data;                // pointer to raw EKG data samples
    }

    // Data object to be sent from analysis application to EKG display device
    private class ekg_results_data {
        float heart_rate_bpm;    // analyzed heart rate in beats per minute
        float heart_rate_var;    // analyzed heart rate variability: SD of successive differences (in beats per minute)
        float hrv_rmssd;         // (not needed externally ???)
        float hrv_sdsd;          // (not needed externally ???)
        float quality_metric;    // overall EKG trace quality indicator
        int fs;                  // sampling frequency of EKG median data
        int nbits;               // number of bits used for each sample; default = 24 bits in 32 bit int (MSB)
        int len;                 // length of median EKG waveform
        int data;               // pointer to normalized median EKG waveform for display
    };

    private class ekg_state_data {
        int fs;                  // sampling frequency
        int nbits;               // number of bits used for each sample; default = 24 bits in 32 bit int (MSB)
        int len;                 // length of raw EKG waveform
        int data;               // pointer to raw EKG data (concatenated chunks)
    };

    private static final Logger logger = Logger.getLogger(SensorDataEndpoint.class.getName());

    private static final int DEFAULT_LIST_LIMIT = 20;

    static {
        // Typically you would register this inside an OfyServive wrapper. See: https://code.google.com/p/objectify-appengine/wiki/BestPractices
        ObjectifyService.register(SensorData.class);
    }

    /**
     * Returns the {@link SensorData} with the corresponding ID.
     *
     * @param id the ID of the entity to be retrieved
     * @return the entity with the corresponding ID
     * @throws NotFoundException if there is no {@code SensorData} with the provided ID.
     */
    @ApiMethod(
            name = "get",
            path = "sensorData/{id}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public SensorData get(@Named("id") Long id) throws NotFoundException {
        logger.info("Getting SensorData with ID: " + id);
        SensorData sensorData = ofy().load().type(SensorData.class).id(id).now();
        if (sensorData == null) {
            throw new NotFoundException("Could not find SensorData with ID: " + id);
        }
        return sensorData;
    }

    /**
     * Inserts a new {@code SensorData}.
     */
    @ApiMethod(
            name = "insert",
            path = "sensorData",
            httpMethod = ApiMethod.HttpMethod.POST)
    public SensorData insert(SensorData sensorData) {
        // Typically in a RESTful API a POST does not have a known ID (assuming the ID is used in the resource path).
        // You should validate that sensorData.id has not been set. If the ID type is not supported by the
        // Objectify ID generator, e.g. long or String, then you should generate the unique ID yourself prior to saving.
        //
        // If your client provides the ID then you should probably use PUT instead.

        Text t = new Text(sensorData.getSensorData());

        logger.info("inserting data");

        // Process algo

        String returnValue = "-1";

        JSONParser parser = new JSONParser();

        try {
            Object obj = parser.parse(sensorData.getSensorData());
            JSONObject jsonObject = (JSONObject) obj;
            String jsonId = (String) jsonObject.get("regId");
            logger.info("json id = "+jsonId);

            JSONArray jsonDataArray = (JSONArray) jsonObject.get("sensor_data_buffer");
            Iterator<JSONObject> iterator = jsonDataArray.iterator();

            Long data;
            ekg_source_data eData = new ekg_source_data();
            ekg_results_data eResult = new ekg_results_data();
            ekg_state_data eState = new ekg_state_data();

            while (iterator.hasNext()) {
                JSONObject jsonData = iterator.next();

                data = (Long)jsonData.get("x");

                eData.data = data.intValue();
                this.ekg_detect(eData, eResult, eState);
            }

            Float bpm = eResult.heart_rate_bpm;
            Float hrv = eResult.heart_rate_var;

            returnValue = "hr="+bpm.toString();

           // returnValue = "done";

        } catch (ParseException e) {
            e.printStackTrace();
        }

        sensorData.setResult(returnValue);

        ofy().save().entity(sensorData).now();
        logger.info("Created SensorData with ID: " + sensorData.getId());

        SensorData returnData = ofy().load().entity(sensorData).now();

        logger.info("sensordata: " + returnData.getSensorData());

        return returnData;

        //return ofy().load().entity(sensorData).now();
    }

    /**
     * Updates an existing {@code SensorData}.
     *
     * @param id         the ID of the entity to be updated
     * @param sensorData the desired state of the entity
     * @return the updated version of the entity
     * @throws NotFoundException if the {@code id} does not correspond to an existing
     *                           {@code SensorData}
     */
    @ApiMethod(
            name = "update",
            path = "sensorData/{id}",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public SensorData update(@Named("id") Long id, SensorData sensorData) throws NotFoundException {
        // TODO: You should validate your ID parameter against your resource's ID here.
        checkExists(id);
        ofy().save().entity(sensorData).now();
        logger.info("Updated SensorData: " + sensorData);
        return ofy().load().entity(sensorData).now();
    }

    /**
     * Deletes the specified {@code SensorData}.
     *
     * @param id the ID of the entity to delete
     * @throws NotFoundException if the {@code id} does not correspond to an existing
     *                           {@code SensorData}
     */
    @ApiMethod(
            name = "remove",
            path = "sensorData/{id}",
            httpMethod = ApiMethod.HttpMethod.DELETE)
    public void remove(@Named("id") Long id) throws NotFoundException {
        checkExists(id);
        ofy().delete().type(SensorData.class).id(id).now();
        logger.info("Deleted SensorData with ID: " + id);
    }

    /**
     * List all entities.
     *
     * @param cursor used for pagination to determine which page to return
     * @param limit  the maximum number of entries to return
     * @return a response that encapsulates the result list and the next page token/cursor
     */
    @ApiMethod(
            name = "list",
            path = "sensorData",
            httpMethod = ApiMethod.HttpMethod.GET)
    public CollectionResponse<SensorData> list(@Nullable @Named("cursor") String cursor, @Nullable @Named("limit") Integer limit) {
        limit = limit == null ? DEFAULT_LIST_LIMIT : limit;
        Query<SensorData> query = ofy().load().type(SensorData.class).limit(limit);
        if (cursor != null) {
            query = query.startAt(Cursor.fromWebSafeString(cursor));
        }
        QueryResultIterator<SensorData> queryIterator = query.iterator();
        List<SensorData> sensorDataList = new ArrayList<SensorData>(limit);
        while (queryIterator.hasNext()) {
            sensorDataList.add(queryIterator.next());
        }
        return CollectionResponse.<SensorData>builder().setItems(sensorDataList).setNextPageToken(queryIterator.getCursor().toWebSafeString()).build();
    }

    private void checkExists(Long id) throws NotFoundException {
        try {
            ofy().load().type(SensorData.class).id(id).safe();
        } catch (com.googlecode.objectify.NotFoundException e) {
            throw new NotFoundException("Could not find SensorData with ID: " + id);
        }
    }


    private void ekg_detect
    (
             ekg_source_data  ekg_in,
             ekg_results_data ekg_out,
             ekg_state_data   ekg_state
    )
    {

                // store output results
                ekg_out.heart_rate_bpm = 60;
                ekg_out.heart_rate_var = 1;
                ekg_out.hrv_rmssd      = 2;
                ekg_out.hrv_sdsd       = 3;



        return;
    }


}

