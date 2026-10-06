package Vibol.SEN.meetingManagements.repository;

import Vibol.SEN.meetingManagements.model.RecurringSeries;
import org.springframework.data.jpa.repository.JpaRepository;


public interface RecurringSeriesRepository extends JpaRepository<RecurringSeries, Long> {
}
