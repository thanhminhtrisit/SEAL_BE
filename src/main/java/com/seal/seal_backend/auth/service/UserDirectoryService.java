package com.seal.seal_backend.auth.service;

import com.seal.seal_backend.auth.dto.response.JudgeResponse;
import com.seal.seal_backend.auth.dto.response.MentorResponse;
import java.util.List;

public interface UserDirectoryService {
    List<JudgeResponse> listJudges();
    List<MentorResponse> listMentors();
}
