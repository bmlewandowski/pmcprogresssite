package com.appspot.pmcprogresssite;

//import com.google.appengine.api.datastore.Text;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import java.util.Date;

@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class Feed {

	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	Long id;
	
	@Persistent
	Long ownerid;
	
	@Persistent
	String ownername;
	
	@Persistent
	String ownerthumb;
	
	@Persistent
	String entitytype;
	
	@Persistent
	String target;
	
	@Persistent
	Long targetid;
	
	@Persistent
	Long targetowner;
	
	@Persistent
	String targetaction;
	
	@Persistent
	String targetthumb;
	
	@Persistent
	java.util.Date created;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	public Long getownerid() {
		return ownerid;
	}

	public void setownerid(Long ownerid) {
		this.ownerid = ownerid;
	}
	
	public String getownername() {
		return ownername;
	}

	public void setownername(String ownername) {
		this.ownername = ownername;
	}
	
	public String getownerthumb() {
		return ownerthumb;
	}

	public void setownerthumb(String ownerthumb) {
		this.ownerthumb = ownerthumb;
	}
	
	public String getentitytype() {
		return entitytype;
	}

	public void setentitytype(String entitytype) {
		this.entitytype = entitytype;
	}
	
	public String gettarget() {
		return target;
	}

	public void settarget(String target) {
		this.target = target;
	}
	
	public Long gettargetid() {
		return targetid;
	}

	public void settargetid(Long targetid) {
		this.targetid = targetid;
	}
	
	public Long gettargetowner() {
		return targetowner;
	}

	public void settargetowner(Long targetowner) {
		this.targetowner = targetowner;
	}
	
	public String gettargetaction() {
		return targetaction;
	}

	public void settargetaction(String targetaction) {
		this.targetaction = targetaction;
	}
	
	public String gettargetthumb() {
		return targetthumb;
	}

	public void settargetthumb(String targetthumb) {
		this.targetthumb = targetthumb;
	}
	
	public Date getcreated() {
		return created;
	}

	public void setcreated(Date created) {
		this.created = created;
	}	
}
