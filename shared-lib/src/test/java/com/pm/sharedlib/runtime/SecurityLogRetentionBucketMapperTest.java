package com.pm.sharedlib.runtime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityLogRetentionBucketMapperTest {

    @Test
    void bucket_shouldDefaultNullToR30() {
        assertThat(SecurityLogRetentionBucketMapper.bucket(null)).isEqualTo("r30");
    }

    @Test
    void bucket_shouldMapUpTo14DaysToR14() {
        assertThat(SecurityLogRetentionBucketMapper.bucket(7)).isEqualTo("r14");
        assertThat(SecurityLogRetentionBucketMapper.bucket(14)).isEqualTo("r14");
    }

    @Test
    void bucket_shouldMapUpTo30DaysToR30() {
        assertThat(SecurityLogRetentionBucketMapper.bucket(15)).isEqualTo("r30");
        assertThat(SecurityLogRetentionBucketMapper.bucket(30)).isEqualTo("r30");
    }

    @Test
    void bucket_shouldMapAbove30DaysToR90() {
        assertThat(SecurityLogRetentionBucketMapper.bucket(31)).isEqualTo("r90");
        assertThat(SecurityLogRetentionBucketMapper.bucket(90)).isEqualTo("r90");
    }

    @Test
    void normalizedDays_shouldDefaultNullTo30AndKeepConfiguredValue() {
        assertThat(SecurityLogRetentionBucketMapper.normalizedDays(null)).isEqualTo(30);
        assertThat(SecurityLogRetentionBucketMapper.normalizedDays(14)).isEqualTo(14);
    }
}
