import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OnlineCoursesAnalyzer {

  List<Course> courses = new ArrayList<>();

  public OnlineCoursesAnalyzer(String datasetPath) {
    BufferedReader br = null;
    String line;
    try {
      br = new BufferedReader(new FileReader(datasetPath, StandardCharsets.UTF_8));
      br.readLine();
      while ((line = br.readLine()) != null) {
        String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
        Course course = new Course(info[0], info[1], new Date(info[2]), info[3], info[4], info[5],
            Integer.parseInt(info[6]), Integer.parseInt(info[7]), Integer.parseInt(info[8]),
            Integer.parseInt(info[9]), Integer.parseInt(info[10]), Double.parseDouble(info[11]),
            Double.parseDouble(info[12]), Double.parseDouble(info[13]), Double.parseDouble(info[14]),
            Double.parseDouble(info[15]), Double.parseDouble(info[16]), Double.parseDouble(info[17]),
            Double.parseDouble(info[18]), Double.parseDouble(info[19]), Double.parseDouble(info[20]),
            Double.parseDouble(info[21]), Double.parseDouble(info[22]));
        courses.add(course);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  //1
  public Map<String, Integer> getPtcpCountByInst() {
    Stream<Course> courseStream = courses.stream();
    return courseStream
        .sorted(Course::compareByIns)
        .collect(Collectors
            .groupingBy(Course::getInstitution, LinkedHashMap::new, Collectors.summingInt(Course::getParticipants)));
  }

  //2
  public Map<String, Integer> getPtcpCountByInstAndSubject() {
    return courses.stream()
        .collect(Collectors.groupingBy(
            course -> course.getInstitution() + "-" + course.getSubject(),
            Collectors.summingInt(Course::getParticipants)
        ))
        .entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
            .thenComparing(Map.Entry.comparingByKey()))
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            (e1, e2) -> e1,
            LinkedHashMap::new
        ));
  }

  //3
  public Map<String, List<List<String>>> getCourseListOfInstructor() {
    Map<String, List<List<String>>> result = new HashMap<>();
    for (Course course : courses) {
      String[] instructors = course.getInstructors().split(", ");
      for (String instructor : instructors) {
        result.putIfAbsent(instructor, new ArrayList<>(Arrays.asList(new ArrayList<>(), new ArrayList<>())));
        if (course.isIndependentlyResponsible()) {
          if (!result.get(instructor).get(0).contains(course.getTitle())) {
            result.get(instructor).get(0).add(course.getTitle());
          }
        } else {
          if (!result.get(instructor).get(1).contains(course.getTitle())) {
            result.get(instructor).get(1).add(course.getTitle());
          }
        }
      }
    }
    for (List<List<String>> lists : result.values()) {
      lists.get(0).sort(String::compareTo);
      lists.get(1).sort(String::compareTo);
    }
    return result;
  }

  //4
  public List<String> getCourses(int topK, String by) {
    List<Course> totalResults = null;
    if (by.equals("hours")) {
      totalResults = courses.stream()
          .sorted(Comparator.comparingDouble(Course::getTotalHours).reversed().thenComparing(Course::getTitle))
          .toList();
    } else if (by.equals("participants")) {
      totalResults = courses.stream()
          .sorted(Comparator.comparingDouble(Course::getParticipants).reversed().thenComparing(Course::getTitle))
          .toList();
    }
    List<String> result = new ArrayList<>();
    int size = 0;
    for (int i = 0; size < topK && i < totalResults.size(); i++) {
      String title = totalResults.get(i).getTitle();
      if (!result.contains(title)) {
        result.add(totalResults.get(i).getTitle());
      }
      size = result.size();
    }
    return result;
  }

  //5
  public List<String> searchCourses(String courseSubject, double percentAudited, double totalCourseHours) {
    List<Course> filteredCourses = courses.stream()
        .filter(c -> c.getSubject().toLowerCase().contains(courseSubject.toLowerCase()))
        .filter(c -> c.getPercentAudited() >= percentAudited)
        .filter(c -> c.getTotalHours() <= totalCourseHours)
        .collect(Collectors.toList());
    Collections.sort(filteredCourses, Comparator.comparing(Course::getTitle));
    List<String> result = new ArrayList<>();
    for (Course c : filteredCourses) {
      String title = c.getTitle();
      if (!result.contains(title)) {
        result.add(title);
      }
    }
    return result;
  }

  //6
  public List<String> recommendCourses(int age, int gender, int isBachelorOrHigher) {
    Map<String, Double> medianAgeMap = new HashMap<>();
    Map<String, Double> malePercentageMap = new HashMap<>();
    Map<String, Double> bachelorPercentageMap = new HashMap<>();
    for (Course course : courses) {
      String courseNumber = course.getNumber();
      double medianAge = course.getMedianAge();
      double malePercentage = course.getPercentMale();
      double bachelorPercentage = course.getPercentDegree();

      medianAgeMap.put(courseNumber,
          medianAgeMap.getOrDefault(courseNumber, 0.0) + medianAge);
      malePercentageMap.put(courseNumber,
          malePercentageMap.getOrDefault(courseNumber, 0.0) + malePercentage);
      bachelorPercentageMap.put(courseNumber,
          bachelorPercentageMap.getOrDefault(courseNumber, 0.0) + bachelorPercentage);
    }
    for (String courseNumber : medianAgeMap.keySet()) {
      int count = 0;
      for (Course course : courses) {
        if (course.getNumber().equals(courseNumber)) {
          count++;
        }
      }
      medianAgeMap.put(courseNumber,
          medianAgeMap.get(courseNumber) / count);
      malePercentageMap.put(courseNumber,
          malePercentageMap.get(courseNumber) / count);
      bachelorPercentageMap.put(courseNumber,
          bachelorPercentageMap.get(courseNumber) / count);
    }
    Map<String, Double> similarityValueMap = new HashMap<>();

    for (String courseNumber : medianAgeMap.keySet()) {
      double avgMedianAge = medianAgeMap.get(courseNumber);
      double avgMalePercentage = malePercentageMap.get(courseNumber);
      double avgBachelorPercentage = bachelorPercentageMap.get(courseNumber);

      double similarityValue = Math.pow(age - avgMedianAge, 2)
          + Math.pow(gender * 100 - avgMalePercentage, 2)
          + Math.pow(isBachelorOrHigher * 100 - avgBachelorPercentage, 2);

      similarityValueMap.put(courseNumber, similarityValue);
    }
    List<String> recommendedCourses = new ArrayList<>();

    Map<String, Course> courseNumberToCourse = new HashMap<>();
    Map<String, Date> courseNumberToEarliestLaunchDate = new HashMap<>();

    for (Course course : courses) {
      String courseNumber = course.getNumber();
      Date launchDate = course.getLaunchDate();

      if (!courseNumberToEarliestLaunchDate.containsKey(courseNumber)
          || launchDate.after(courseNumberToEarliestLaunchDate.get(courseNumber))) {
        courseNumberToCourse.put(courseNumber, course);
        courseNumberToEarliestLaunchDate.put(courseNumber, launchDate);
      }
    }

    List<Course> courseEarliestLaunch = new ArrayList<>(courseNumberToCourse.values());

    courseEarliestLaunch
        .sort(Comparator.comparingDouble((Course c) -> similarityValueMap.get(c.getNumber())).thenComparing(Course::getTitle));

    for (Course course : courseEarliestLaunch) {
      if (recommendedCourses.size() >= 10) {
        break;
      }
      String title = course.getTitle();
      if (!recommendedCourses.contains(title)) {
        recommendedCourses.add(title);
      }
    }
    return recommendedCourses;
  }

}

class Course {
  String institution;
  String number;
  Date launchDate;
  String title;
  String instructors;
  String subject;
  int year;
  int honorCode;
  int participants;
  int audited;
  int certified;
  double percentAudited;
  double percentCertified;
  double percentCertified50;
  double percentVideo;
  double percentForum;
  double gradeHigherZero;
  double totalHours;
  double medianHoursCertification;
  double medianAge;
  double percentMale;
  double percentFemale;
  double percentDegree;

  public Course(String institution, String number, Date launchDate,
                String title, String instructors, String subject,
                int year, int honorCode, int participants,
                int audited, int certified, double percentAudited,
                double percentCertified, double percentCertified50,
                double percentVideo, double percentForum, double gradeHigherZero,
                double totalHours, double medianHoursCertification,
                double medianAge, double percentMale, double percentFemale,
                double percentDegree) {
    this.institution = institution;
    this.number = number;
    this.launchDate = launchDate;
    if (title.startsWith("\"")) title = title.substring(1);
    if (title.endsWith("\"")) title = title.substring(0, title.length() - 1);
    this.title = title;
    if (instructors.startsWith("\"")) instructors = instructors.substring(1);
    if (instructors.endsWith("\"")) instructors = instructors.substring(0, instructors.length() - 1);
    this.instructors = instructors;
    if (subject.startsWith("\"")) subject = subject.substring(1);
    if (subject.endsWith("\"")) subject = subject.substring(0, subject.length() - 1);
    this.subject = subject;
    this.year = year;
    this.honorCode = honorCode;
    this.participants = participants;
    this.audited = audited;
    this.certified = certified;
    this.percentAudited = percentAudited;
    this.percentCertified = percentCertified;
    this.percentCertified50 = percentCertified50;
    this.percentVideo = percentVideo;
    this.percentForum = percentForum;
    this.gradeHigherZero = gradeHigherZero;
    this.totalHours = totalHours;
    this.medianHoursCertification = medianHoursCertification;
    this.medianAge = medianAge;
    this.percentMale = percentMale;
    this.percentFemale = percentFemale;
    this.percentDegree = percentDegree;
  }

  public String getInstitution() {
    return institution;
  }

  public String getNumber() {
    return number;
  }

  public int getParticipants() {
    return participants;
  }

  public String getSubject() {
    return subject;
  }

  public String getInstructors() {
    return instructors;
  }

  public String getTitle() {
    return title;
  }

  public Date getLaunchDate() {
    return launchDate;
  }

  public double getTotalHours() {
    return totalHours;
  }

  public double getPercentAudited() {
    return percentAudited;
  }

  public double getMedianAge() {
    return medianAge;
  }

  public double getPercentMale() {
    return percentMale;
  }

  public double getPercentDegree() {
    return percentDegree;
  }


  public static int compareByIns(Course c1, Course c2) {
    return c1.institution.compareTo(c2.institution);
  }

  public boolean isIndependentlyResponsible() {
    String[] instructor = instructors.split(", ");
    return instructor.length == 1;
  }
}
