package com.bandmong.jam.dto;

import java.util.List;

public record JamListResponse(List<JamView> upcoming, List<JamView> past) {
}
