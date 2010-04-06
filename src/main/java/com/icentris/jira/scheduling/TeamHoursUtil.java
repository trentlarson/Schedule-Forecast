package com.icentris.jira.scheduling;

import java.util.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.hibernate.*;
import org.hibernate.cfg.*;
import com.icentris.sql.SimpleSQL;
import com.icentris.jira.dao.TeamHours;
import com.icentris.jira.dao.Team;
import com.icentris.jira.helper.ForecastUtil;


/**
TeamHours hours = new TeamHours();
hours.setTeamId(new Long(100));
//hours.setUserName("lester.tester");
hours.setStartOfWeek(new Date());
hours.setHoursAvailable(new Integer(30));

sess = HibernateUtil.currentSession();
tx = sess.beginTransaction();
sess.save(hours);
tx.commit();
HibernateUtil.closeSession();


Team team = new Team();
team.setId(new Long(100));
team.setName("Professional Services");
team.setProjectId(new Long(10000));

sess = HibernateUtil.currentSession();
tx = sess.beginTransaction();
sess.save(team);
tx.commit();
HibernateUtil.closeSession();
**/

public class TeamHoursUtil {

  public static final int INITIAL_YEAR = 2000;
  /** First day of work within a week */
  public static final int FIRST_DAY_OF_WORK = Calendar.MONDAY;

  /**
     @return the week number offset from INITIAL_YEAR for this week
   */
  public static int weekNumber() {
      return weekNumber(new GregorianCalendar());
  }

  /**
     @return the week number offset from INITIAL_YEAR for the given date
   */
  public static int weekNumber(GregorianCalendar cal) {
    int yearNum = cal.get(Calendar.YEAR) - INITIAL_YEAR;
    return (yearNum * 52) + cal.get(Calendar.WEEK_OF_YEAR);
  }

  /**
     @return the week number offset from INITIAL_YEAR for the given date
   */
  public static int weekNumber(Date date) {
    GregorianCalendar newCal = new GregorianCalendar();
    newCal.setTime(date);
    return weekNumber(newCal);
  }

  /**
     @return the week start-date that is weekNumber offset from INITIAL_YEAR
   */
  public static GregorianCalendar weekCal(int weekNumber) {
    int yearNum = INITIAL_YEAR + (weekNumber / 52);
    int weekNum = weekNumber % 52;
    GregorianCalendar cal = new GregorianCalendar();
    cal.set(Calendar.YEAR, yearNum);
    cal.set(Calendar.WEEK_OF_YEAR, weekNum);
    return weekStart(cal);
  }

    /**
       @return the starting moment of the current week
     */
  public static GregorianCalendar weekStart() {
      return weekStart(new GregorianCalendar());
  }
    /**
       @return the starting moment of the week containing the given date
       Note that it is set to the FIRST_DAY_OF_WORK.
     */
  public static GregorianCalendar weekStart(GregorianCalendar cal) {
      GregorianCalendar newCal = (GregorianCalendar) cal.clone();
      newCal.set(Calendar.DAY_OF_WEEK, FIRST_DAY_OF_WORK);
      newCal.set(Calendar.HOUR_OF_DAY, 0);
      newCal.set(Calendar.MINUTE, 0);
      newCal.set(Calendar.SECOND, 0);
      newCal.set(Calendar.MILLISECOND, 0);
      return newCal;
  }


  public static List<Team> getTeams() {
    Session sess = HibernateUtil.currentSession();
    Transaction tx = sess.beginTransaction();
    List<Team> allTeams = sess.createQuery("from Team order by name").list();
    HibernateUtil.closeSession();
    return allTeams;
  }

  /**
     Map from project ID Long to project name String.
   */
  public static Map<Long,String> getProjects() {
    Map result = new HashMap();
    Connection conn = null;
    ResultSet rset = null;
    try {
      conn = ForecastUtil.getConnection();
      rset = SimpleSQL.executeQuery("select id, pname as name from project", new Object[0], conn);
      while (rset.next()) {
        result.put(new Long(rset.getLong("id")), rset.getString("name"));
      }
    } catch (SQLException e) {
        throw new java.lang.reflect.UndeclaredThrowableException(e);
    } finally {
      try { rset.close(); } catch (Exception e) {}
      try { conn.close(); } catch (Exception e) {}
    }
    return result;
  }


  /**
     @return a Map from each team ID Long to Teams.WeeksAndUsers for that week
     (see Teams.WeeksAndUsers for the contract of that class)

     @deprecated see TimeScheduleLoader.loadUserWeeklyHours, and team-hours.jsp for usage
     ... besides, it looks like it's broken (with error IndexOutOfBoundsException)
   */
  public static Map retrieveWeeks(int firstWeekNum, int numWeeks) {
    if (numWeeks < 1) {
      throw new IllegalStateException("You cannot ask for less than 1 week of results; you asked for " + numWeeks + ".");
    }

    Session sess = HibernateUtil.currentSession();
    List allTeams = sess.createQuery("from Team").list();
    HibernateUtil.closeSession();

    Timestamp firstWeekStart = new Timestamp(weekCal(firstWeekNum).getTime().getTime());
    Timestamp lastWeekEnd = new Timestamp(weekCal(firstWeekNum + numWeeks).getTime().getTime());

    Map result = new HashMap();
    int teamNum = -1;
    for (Iterator teamIter = allTeams.iterator(); teamIter.hasNext(); ) {
      Team team = (Team) teamIter.next();
      teamNum++;

      // make list with all usernames who have any entries in this range,
      //   not including null (for team)
      SortedSet usersInOrder = new TreeSet();
      Connection conn = null;
      ResultSet rset = null;
      try {
        conn = ForecastUtil.getConnection();
        Set userSet = new TreeSet();
        String sql =
          "select distinct username from team_hours "
          + " where team_id = ? and ? <= start_of_week and start_of_week < ?";
        Object[] args = { team.getId(), firstWeekStart, lastWeekEnd };
        rset = SimpleSQL.executeQuery(sql, args, conn);
        while (rset.next()) {
          usersInOrder.add(rset.getString("username"));
        }
      } catch (SQLException e) {
        throw new java.lang.reflect.UndeclaredThrowableException(e);
      } finally {
        try { rset.close(); } catch (Exception e) {}
        try { conn.close(); } catch (Exception e) {}
      }

      // loop through all weeks and add data
      SortedMap<Date,List<TeamHours>> weekHours =
        new TreeMap<Date,List<TeamHours>>();
      SortedMap<Teams.UserTimeKey,List<TeamHours>> teamUserHours =
        new TreeMap<Teams.UserTimeKey,List<TeamHours>>();
      for (int weekNum = firstWeekNum; weekNum < firstWeekNum + numWeeks; weekNum++) {

        // create a list with default hours so that everyone has a default entry
        ArrayList defaultTimes = new ArrayList();
        for (Iterator usernameIter = usersInOrder.iterator(); usernameIter.hasNext(); ) {
          TeamHours hours = new TeamHours();
          hours.setTeamId(team.getId());
          hours.setUsername((String) usernameIter.next());
          hours.setStartOfWeek(weekCal(weekNum).getTime());
          hours.setHoursAvailable(new Double(0));
          defaultTimes.add(hours);
        }
        
        sess = HibernateUtil.currentSession();
        List dbWeekHours = sess
          .createQuery("from TeamHours where teamId = :teamId and startOfWeek = :startOfWeek")
          .setParameter("teamId", team.getId())
          .setParameter("startOfWeek", weekCal(weekNum).getTime())
          .list();
        HibernateUtil.closeSession();

        // sort all records so we can fill in the right order
        Collections.sort(dbWeekHours, new Teams.HoursUserComparator());

        // use the defaultTimes but replace with real values when available
        List thisWeekHours = (List) defaultTimes.clone();
        int weekNumToFill = 0;
        Iterator dbHoursIter = dbWeekHours.iterator();
        if (dbHoursIter.hasNext()) {
          TeamHours dbHours = (TeamHours) dbHoursIter.next();
          TeamHours defaultHours = (TeamHours) thisWeekHours.get(weekNumToFill);
          if (dbHours.getUsername() == null) {
            thisWeekHours.set(weekNumToFill, dbHours);
          }
          weekNumToFill++;
          for (; dbHoursIter.hasNext(); ) {
            dbHours = (TeamHours) dbHoursIter.next();
            defaultHours = (TeamHours) thisWeekHours.get(weekNumToFill);
            while (!defaultHours.getUsername().equals(dbHours.getUsername())) {
              weekNumToFill++;
              defaultHours = (TeamHours) thisWeekHours.get(weekNumToFill);
            }
            thisWeekHours.set(weekNumToFill, dbHours);
          }
          weekHours.put(weekCal(weekNum).getTime(), thisWeekHours);
        }

        // fill in the team-user records
        TeamHours[] hoursArray = (TeamHours[]) thisWeekHours.toArray(new TeamHours[0]);
        for (int i = 0; i < hoursArray.length; i++) {
          Teams.UserTimeKey key = 
            new Teams.UserTimeKey(hoursArray[i].getTeamId(), hoursArray[i].getUsername());
          // must insert (just the first time through)
          if (!teamUserHours.containsKey(key)) {
            teamUserHours.put(key, new ArrayList());
          }
          ((List) teamUserHours.get(key)).add(hoursArray[i]);
        }
      }

      result.put(team.getId(), new Teams.WeeksAndUsers(weekHours, teamUserHours));
    }
    
    return result;
  }



public static class HibernateUtil {

    private static final SessionFactory sessionFactory;

    static {
        try {
            // Create the SessionFactory
            sessionFactory = new Configuration().configure().buildSessionFactory();
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static final ThreadLocal session = new ThreadLocal();

    public static Session currentSession() throws HibernateException {
        Session s = (Session) session.get();
        // Open a new Session, if this Thread has none yet
        if (s == null) {
            s = sessionFactory.openSession();
            session.set(s);
        }
        return s;
    }

    public static void closeSession() throws HibernateException {
        Session s = (Session) session.get();
        session.set(null);
        if (s != null)
            s.close();
    }
}

}
