package ru.hvayon.BonusService.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import ru.hvayon.BonusService.domain.Privilege;
import ru.hvayon.BonusService.domain.PrivilegeHistory;
import ru.hvayon.BonusService.model.PrivilegeHistoryRecordRequest;
import ru.hvayon.BonusService.repository.PrivilegeHistoryRepository;
import ru.hvayon.BonusService.repository.PrivilegeRepository;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PrivilegeServiceImpl implements PrivilegeService {

    private final PrivilegeRepository privilegeRepository;
    private final PrivilegeHistoryRepository privilegeHistoryRepository;

    public PrivilegeServiceImpl(PrivilegeRepository privilegeRepository, PrivilegeHistoryRepository privilegeHistoryRepository) {
        this.privilegeRepository = privilegeRepository;
        this.privilegeHistoryRepository = privilegeHistoryRepository;
    }

    @Override
    public Privilege getPrivilege(String username) {
        Optional<Privilege> privilege = privilegeRepository.findByUsername(username);
        if (privilege.isPresent()) {
            return privilege.get();
        } else {
            throw new EntityNotFoundException("Privilege of user " + username + " not found");
        }
    }

    @Override
    public List<PrivilegeHistory> getPrivilegeHistory(String username) {
        return privilegeHistoryRepository.findAllByPrivilege(getPrivilege(username));
    }

    @Override
    public int addHistoryRecord(String username, PrivilegeHistoryRecordRequest request) {
        PrivilegeHistory record = PrivilegeHistory.build(
                0,
                getPrivilege(username),
                request.getTicketUid(),
                LocalDateTime.now().toString(),
                request.getBalanceDiff(),
                request.getOperationType()
        );
        privilegeHistoryRepository.save(record);
        return record.getId();
    }

    @Override
    public Privilege updatePrivilege(String username, Map<String, Object> fields) {
        Optional<Privilege> privilege = privilegeRepository.findByUsername(username);
        if (privilege.isPresent()) {
            fields.forEach((key, value) -> {
                Field field = ReflectionUtils.findField(Privilege.class, key);
                field.setAccessible(true);
                ReflectionUtils.setField(field, privilege.get(), value);
            });
            return privilegeRepository.save(privilege.get());
        }
        return null;
    }

    @Override
    public PrivilegeHistory getPrivilegeHistoryOfTicket(String username, UUID ticketUid) {
        Optional<PrivilegeHistory> privilegeHistory =
                privilegeHistoryRepository.findTopByPrivilegeAndTicketUidOrderByDate(
                        getPrivilege(username),
                        ticketUid
                );
        if (privilegeHistory.isPresent()) {
            return privilegeHistory.get();
        } else {
            throw new EntityNotFoundException("Privilege history of ticket with uid=" + ticketUid + " not found");
        }
    }
}
