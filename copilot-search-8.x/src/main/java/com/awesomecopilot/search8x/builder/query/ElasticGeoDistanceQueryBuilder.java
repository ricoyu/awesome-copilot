package com.awesomecopilot.search8x.builder.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

/**
 * Geo Distance Query Builder for ES 8.x
 *
 * @author Rico Yu ricoyu520@gmail.com
 */
public class ElasticGeoDistanceQueryBuilder extends BaseQueryBuilder {

    private String distance;
    private double lat;
    private double lon;
    private String locationField = "location";

    public ElasticGeoDistanceQueryBuilder(String... indices) {
        super(indices);
    }

    public ElasticGeoDistanceQueryBuilder distance(String distance) {
        this.distance = distance;
        return this;
    }

    public ElasticGeoDistanceQueryBuilder location(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
        return this;
    }

    public ElasticGeoDistanceQueryBuilder locationField(String field) {
        this.locationField = field;
        return this;
    }

    public ElasticGeoDistanceQueryBuilder includeSources(String... fields) {
        this.includeSource = fields;
        return this;
    }

    @Override
    protected Query buildQuery() {
        return Query.of(q -> q.geoDistance(gd -> gd
                .field(locationField)
                .location(gl -> gl.latlon(ll -> ll.lat(lat).lon(lon)))
                .distance(distance)
        ));
    }
}
