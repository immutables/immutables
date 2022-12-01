package issue1387.org.immutables.criteria.mongo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.criteria.Criteria;
import org.immutables.value.Value;

import java.util.UUID;

@Value.Immutable
@Criteria.Repository
@Criteria
@JsonSerialize(as = ImmutableSample.class)
@JsonDeserialize(as = ImmutableSample.class)
 public interface Sample{

    @Criteria.Id
    @JsonProperty("_id")
    public String getId();


    //getter
    public String getName();

    //property name
    public String surname();


    public static Sample createRandom() {
        final Sample sample = Sample.builder()
                .id(UUID.randomUUID().toString())
                .name("Name " + System.currentTimeMillis())
                .surname("Surname " + System.currentTimeMillis())
                .build();
        return sample;
    }

    /**
     *
     */
    public static final class Builder extends ImmutableSample.Builder {
        Builder() {

        }
    }

    /**
     * Get the builder
     *
     * @return
     */
    public static Builder builder() {
        return new Builder();
    }
}
