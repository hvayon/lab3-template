package ru.hvayon.BonusService.service;

import org.springframework.stereotype.Service;
import ru.hvayon.BonusService.domain.Privilege;
import ru.hvayon.BonusService.domain.PrivilegeHistory;
import ru.hvayon.BonusService.model.PrivilegeHistoryRecordRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public interface PrivilegeService {

    public Privilege getPrivilege(String username);

    public List<PrivilegeHistory> getPrivilegeHistory(String username);

    public int addHistoryRecord(String username, PrivilegeHistoryRecordRequest request);

    public Privilege updatePrivilege(String username, Map<String, Object> fields);

    public PrivilegeHistory getPrivilegeHistoryOfTicket(String username, UUID ticketUid);
}
