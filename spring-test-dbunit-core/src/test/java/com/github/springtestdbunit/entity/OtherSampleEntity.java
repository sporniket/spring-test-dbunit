package com.github.springtestdbunit.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;

/**
 * @author Oleksii Lomako
 */
@Entity
public class OtherSampleEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
	@GenericGenerator(name = "native", strategy = "native")
	private Integer id;

	@Column
	private String value;

	public String getValue() {
		return value;
	}

	public void setValue(String newValue) {
		value = newValue;
	}

	@Override
	public String toString() {
		return "OtherSampleEntity{" + "id=" + id + ", value='" + value + '\'' + '}';
	}

}
