package hibernate.test;

import hibernate.test.dto.DepartmentEntity;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

public class TestHibernateEhcache {
	
	public static void main(String[] args) 
	{
		//storeData();
		
		//Open the hibernate session
		Session session = HibernateUtil.getSessionFactory().openSession();
		session.beginTransaction();
		try
		{
		    DepartmentEntity department= null;
		    
			//Entity is fecthed very first time
//			DepartmentEntity department = (DepartmentEntity) session.load(DepartmentEntity.class, new Integer(1));
//			System.out.println(department.getName());
//			
//			//fetch the department entity again
//			department = (DepartmentEntity) session.load(DepartmentEntity.class, new Integer(1));
//			System.out.println(department.getName());
//			
//			session.evict(department);
//			
//			department = (DepartmentEntity) session.load(DepartmentEntity.class, new Integer(1));
//			System.out.println(department.getName());
//			
			Session anotherSession = HibernateUtil.getSessionFactory().openSession();
			anotherSession.beginTransaction();
			
//			department = (DepartmentEntity) anotherSession.load(DepartmentEntity.class, new Integer(1));
//			System.out.println(department.getName());
			
			
			System.out.println(anotherSession.createCriteria(DepartmentEntity.class).add(Restrictions.eq("name", "Human Resource")).setCacheable(true).list().size());
			
			System.out.println(anotherSession.createCriteria(DepartmentEntity.class).add(Restrictions.eq("name", "Human Resource")).setCacheable(true).list().size());
                        
			System.out.println(anotherSession.createCriteria(DepartmentEntity.class).add(Restrictions.eq("name", "Human Resource")).setCacheable(true).list().size());
                        
			anotherSession.getTransaction().commit();
			
		}
		finally
		{
			System.out.println(HibernateUtil.getSessionFactory().getStatistics().getQueryCacheHitCount());
			System.out.println(HibernateUtil.getSessionFactory().getStatistics().getSecondLevelCacheHitCount());

			System.out.println(HibernateUtil.getSessionFactory().getStatistics().getSecondLevelCacheStatistics("org.hibernate.cache.StandardQueryCache"));

			session.getTransaction().commit();
			HibernateUtil.shutdown();
		}
	}
	
	private static void storeData()
	{
		Session session = HibernateUtil.getSessionFactory().openSession();
		session.beginTransaction();
		
		DepartmentEntity department = new DepartmentEntity();
		department.setName("Human Resource");
		department.setId(1);
		session.save(department);

		session.getTransaction().commit();
	}

}
